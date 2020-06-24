(ns receive.handlers.file-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest is use-fixtures testing are]]
            [hiccup.core :as h]
            [receive.handlers.file :as handler]
            [receive.service.files :as file-service]
            [receive.service.files-test :as files]
            [ring.mock.request :as mock]
            [receive.config :refer [config]]))

(def tempfile-name "tempfile.dat")
(def tempfolder-path "/tmp/")
(def tempfile-path (str tempfolder-path tempfile-name))

(defn tempfile->file [tempfile]
  {:tempfile tempfile
   :content-type "text/plain"
   :filename (.getName tempfile)
   :size (.length tempfile)})

(defn mock-upload-request [file]
  (-> (mock/request :post "/upload/")
      (assoc :content-type "multipart/form-data"
             :params {:file file}
             :multipart-params {:file file}
             :body (io/input-stream (:tempfile file)))))

(defn mock-upload-response [file]
  (handler/upload (mock-upload-request file)))

(deftest upload-handler
  (with-redefs [handler/uuid-str
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/save-file
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")]
    (let [tempfile (files/create-temp-file tempfile-path)
          file (tempfile->file tempfile)
          mock-response (mock-upload-response file)]
      (is (= mock-response
             {:status 200
              :body {:name tempfile-name
                     :success true
                     :message "File saved successfully!"
                     :uid "958a5425-060b-4aad-ba65-bf25e4458991"}})))))

(deftest download-link-test
  (with-redefs [handler/uuid-str (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/get-absolute-filename (constantly "/tmp/tempfile.dat")]
    (let [uid (handler/uuid-str)
          mock-request (-> (mock/request :get (format "/download/api/%s/" uid))
                           (assoc :params {:id uid}))
          mock-response (handler/download-file mock-request)]
      (files/create-temp-file tempfile-path)
      (is (= (:status mock-response) 200))
      (is (-> mock-response :body (.exists))))))

(deftest download-ui-link-test
  (with-redefs [handler/uuid-str (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/get-filename (constantly "tempfile.dat")]
    (let [uid "958a5425-060b-4aad-ba65-bf25e4458991"
          mock-request (-> (mock/request :get (format "/download/%s/" uid))
                           (assoc :params {:id uid}))
          mock-response (handler/download-view mock-request)
          headers (:headers mock-response)
          status (:status mock-response)]
      (is (= headers {"Content-Type" "text/html"}))
      (is (= status 200)))))

(deftest download-ui-expired-test
  (with-redefs [file-service/get-filename (constantly {:error :file-expired})]
    (let [uid (handler/uuid-str)
          mock-request (assoc
                        (mock/request :get (format "/download/%s/" uid))
                        :params {:id uid})
          mock-response (handler/download-view mock-request)
          status (:status mock-response)
          body (:body mock-response)]
      (is (= status 410))
      (is (string/includes? body
                            "<h1>410</h1><span>Link has expired</span>")))))

(deftest download-ui-bad-link-test
  (with-redefs [file-service/find-file (constantly nil)]
    (let [uid (handler/uuid-str)
          mock-request (assoc
                        (mock/request :get (format "/download/%s/" uid))
                        :params {:id uid})
          mock-response (handler/download-view mock-request)
          status (:status mock-response)
          body (:body mock-response)]
      (is (= status 404))
      (is (string/includes? body
                            "<h1>404</h1><span>File not found</span>")))))

(deftest download-link-bad-uuid-test
  (with-redefs [file-service/find-file (constantly nil)]
    (let [uid "bad_uuid"
          mock-request (assoc
                        (mock/request :get (format "/api/download/%s/" uid))
                        :params {:id uid})
          mock-response (handler/download-file mock-request)
          status (:status mock-response)
          body (:body mock-response)]
      (is (= status 400))
      (is (= body {:success false, :message "Not valid UUID"})))))

(deftest download-ui-bad-uuid-test
  (with-redefs [file-service/find-file (constantly nil)]
    (let [uid "bad_uuid"
          mock-request (assoc
                        (mock/request :get (format "/download/%s/" uid))
                        :params {:id uid})
          mock-response (handler/download-view mock-request)
          status (:status mock-response)
          body (:body mock-response)]
      (is (= status 400))
      (is (string/includes? body
                            "<h1>400</h1><span>Not valid UUID</span>")))))

(deftest share-handler
  (let [tempfile (files/create-temp-file "/tmp/tempfile.dat")
        file (tempfile->file tempfile)
        upload-response (mock-upload-response file)
        share-url (format "/share?uid=%s" (-> upload-response :body :uid))
        mock-request (mock/request :get share-url)]
    (is (= (:status (handler/share-handler mock-request)) 200))))

(deftest index-handler
  (let [mock-response (handler/index (mock/request :get "/"))
        mock-status (:status mock-response)]
    (is (= mock-status 200))))

(deftest upload-validate-file-too-big
  (with-redefs
   [file-service/save-file (constantly "file.dat")]
    (let [file (-> "/tmp/tempfile.dat"
                   files/create-temp-file
                   tempfile->file
                   (assoc :size (inc (:max-file-size config))))
          mock-request (mock-upload-request file)]
      (is (= (handler/upload mock-request)
             {:status 413
              :body {:success false
                     :message "File too big"}})))))

(deftest upload-validate-file-exists
  (with-redefs
   [file-service/save-file (constantly "file.dat")]
    (let [file (-> "/tmp/tempfile.dat"
                   files/create-temp-file
                   tempfile->file
                   (assoc :size 0))
          mock-request (mock-upload-request file)]
      (is (= (handler/upload mock-request)
             {:status 400
              :body {:success false
                     :message "File not provided"}})))))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(deftest upload-validate-filename
  (with-redefs
   [file-service/save-file (constantly "file.dat")]
    (let [filename (rand-str (inc (:max-filename-length config)))
          file (-> "/tmp/tempfile.dat"
                   files/create-temp-file
                   tempfile->file
                   (assoc :filename filename))
          mock-request (mock-upload-request file)]
      (is (= (handler/upload mock-request)
             {:status 400
              :body {:success false
                     :message "File name is too long"}})))))

(def get-uploaded-files-data
  [#:file_storage{:id 194
                  :filename "saber1.png"
                  :uid "3b24ceb1-42cd-459b-ba74-8a82dad5cbb6"
                  :created_at #inst "2020-06-09T09:13:34.396941000-00:00"
                  :user_id 111}
   #:file_storage{:id 195
                  :filename "saber2.png"
                  :uid "33327486-9830-4c16-bbae-995d695195aa"
                  :created_at #inst "2020-06-09T09:13:58.564455000-00:00"
                  :user_id 111}])

(defn filename->file-div [filename]
  [:div {:class "filename"}
   [:span "Filename"]
   [:h4 filename]])

(deftest uploaded-files-test
  (testing "should return list of files that were uploaded by the user"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (let [mock-request (-> (mock/request :get "/api/user/files")
                             (assoc :auth {:user_id 111}))
            response (handler/uploaded-files mock-request)]
        (is (= (:status response) 200))
        (are [response html] (string/includes? response html)
          (:body response) (h/html (filename->file-div "saber1.png"))
          (:body response) (h/html (filename->file-div "saber1.png")))))))

(defn cleanup-tempfile [f]
  (f)
  (io/delete-file tempfile-path))

;; TODO: Add functionality to use fixtures per test
(use-fixtures :once cleanup-tempfile)
