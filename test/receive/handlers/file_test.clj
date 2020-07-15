(ns receive.handlers.file-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :refer [deftest is use-fixtures testing are]]
   [clj-time.core :as time]
   [hiccup.core :as h]
   [receive.config :refer [config]]
   [receive.error-handler :refer [error]]
   [receive.handlers.file :as handler]
   [receive.model.file :as file-model]
   [receive.service.files :as file-service]
   [receive.service.user :as user-service]
   [receive.service.files-test :as files]
   [ring.mock.request :as mock])
  (:import
   java.util.UUID))

(def tempfile-name "tempfile.dat")
(def tempfolder-path "/tmp/")
(def tempfile-path (str tempfolder-path tempfile-name))

(defn tempfile->file [tempfile]
  {:tempfile tempfile
   :content-type "text/plain"
   :filename (.getName tempfile)
   :size (.length tempfile)})

(def get-uploaded-files-data
  [{:id 194
    :filename "saber1.png"
    :uid "3b24ceb1-42cd-459b-ba74-8a82dad5cbb6"
    :created-at #inst "2020-06-09T09:13:34.396941000-00:00"
    :user-id 111}
   {:id 195
    :filename "saber2.png"
    :uid "33327486-9830-4c16-bbae-995d695195aa"
    :created-at #inst "2020-06-09T09:13:58.564455000-00:00"
    :user-id 111}])

(defn filename->file-div [filename]
  [:div {:class "filename"}
   [:span "Filename"]
   [:h4 filename]])

(def shared-with-user-details
  [{:id 69
    :first_name "email@something.com"
    :last_name nil
    :email "email@something.com"
    :dt_created #inst "2020-06-19T09:36:57.277049000-00:00"
    :dt_updated #inst "2020-06-19T09:36:57.277049000-00:00"
    :status "unregistered"}])

(def update-file-result
  {:filename "image1.png"
   :uid #uuid "94c22936-b0bb-11ea-9e05-4c32759dd39d"
   :dt-created #inst "2020-06-17T16:57:08.931927000-00:00"
   :dt-expire #inst "2020-06-19T16:57:08.930000000-00:00"
   :dt-updated #inst "2020-06-17T17:58:06.365726000-00:00"
   :is-private true
   :shared-with-users [11 12]
   :owner-id 10})

(def find-file-result
  {:filename "image1.png"
   :owner-id 10
   :shared-with-users []
   :expired false})

(def uploaded-file-response
  {:status 200
   :body {:success true
          :data ({:filename "saber.png"
                  :uid "3b24ceb1-42cd-459b-ba74-8a82dad5cbb6", :created_at #inst "2020-06-09T09:13:34.396-00:00"}
                 {:filename "saber.png"
                  :uid "33327486-9830-4c16-bbae-995d695195aa"
                  :created_at #inst "2020-06-09T09:13:58.564-00:00"})}})

(defn mock-upload-request [file]
  (-> (mock/request :post "/upload/")
      (assoc :content-type "multipart/form-data"
             :params {:file file}
             :multipart-params {:file file}
             :body (io/input-stream (:tempfile file)))))

(defn mock-upload-response [file]
  (handler/upload (mock-upload-request file)))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn cleanup-tempfile [f]
  (f)
  (io/delete-file tempfile-path))

(use-fixtures :once cleanup-tempfile)

(deftest upload-handler
  (with-redefs [handler/uuid-str
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/save-file
                (constantly {:uid "958a5425-060b-4aad-ba65-bf25e4458991"})]
    (let [tempfile (files/create-temp-file tempfile-path)
          file (tempfile->file tempfile)
          mock-response (mock-upload-response file)]
      (is (= mock-response
             {:status 200
              :body {:success true
                     :data {:uid "958a5425-060b-4aad-ba65-bf25e4458991"}}})))))

(deftest download-link-test
  (with-redefs [handler/uuid-str (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/get-filename (constantly "tempfile.dat")
                file-service/file-save-path (constantly "/tmp/tempfile.dat")]
    (let [uid (handler/uuid-str)
          mock-request (-> (mock/request :get (format "/download/api/%s/" uid))
                           (assoc :params {:id uid}))
          mock-response (handler/download-file mock-request)]
      (files/create-temp-file tempfile-path)
      (are [key value] (= (key mock-response) value)
        :status 200
        :headers {"Content-Disposition"
                  "attachment; filename=\"tempfile.dat\""})
      (is (-> mock-response :body .exists)))))

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
  (with-redefs [file-service/get-filename (constantly (error :file-expired))]
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
  (with-redefs [file-model/find-file (constantly nil)]
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
  (with-redefs [file-model/find-file (constantly nil)]
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
  (with-redefs [file-model/find-file (constantly nil)]
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

(deftest uploaded-files-ui-test
  (testing "should return list of files that were uploaded by the user"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (let [mock-request (-> (mock/request :get "/api/user/files")
                             (assoc :auth {:user-id 111}))
            response (handler/uploaded-files-ui mock-request)]
        (is (= (:status response) 200))
        (are [response html] (string/includes? response html)
          (:body response) (h/html (filename->file-div "saber1.png"))
          (:body response) (h/html (filename->file-div "saber1.png")))))))

(deftest update-file-test
  (with-redefs [user-service/find-or-create (constantly [{:id 11} {:id 12}])
                file-service/update-file-data (constantly update-file-result)
                file-model/find-file (constantly find-file-result)]
    (let [uid (str (:uid update-file-result))
          mock-request (-> (mock/request :patch (str "/api/user/files/" uid))
                           (assoc :params {:is_private true
                                           :shared_with_users ["email1@nilenso.com"
                                                               "email2@nilenso.com"]
                                           :dt-expire (time/now)}
                                  :route-params {:id uid}
                                  :auth {:user-id 10}))
          mock-response (handler/update-file mock-request)
          status (:status mock-response)
          is-private (-> mock-response :body :data :is-private)]
      (is (= 200 status))
      (is (true? is-private)))))

(deftest update-file-validtion-test
  (testing "should return an error when domain locked and emails are not on the same domain"
    (with-redefs [user-service/find-or-create (constantly nil)
                  file-service/update-file-data (constantly nil)
                  file-model/find-file (constantly nil)
                  config  {:domain-locked true :domain "nilenso.com"}]
      (let [uid (str (UUID/randomUUID))
            mock-request (-> (mock/request :put (str "/api/user/files/" uid))
                             (assoc :params {:is_private true
                                             :shared_with_users
                                             ["email1@non-nilenso.com"
                                              "email2@nilenso.com"]}
                                    :route-params {:id uid}
                                    :auth {:user-id 10}))
            mock-response (handler/update-file mock-request)]
        (is (= 400 (:status mock-response)))
        (is (= "Only same domain emails allowed"

               (-> mock-response :body :message)))))))

(deftest update-file-email-validation-test
  (testing "should return an error if emails are not valid"
    (with-redefs [user-service/find-or-create (constantly nil)
                  file-service/update-file-data (constantly nil)
                  file-model/find-file (constantly nil)
                  config  {:domain-locked false}]
      (let [uid (str (:uid update-file-result))
            mock-request (-> (mock/request :put (str "/api/user/files/" uid))
                             (assoc :params {:is_private true
                                             :shared_with_users
                                             ["bad_email"
                                              "email2@gmail.com"]}
                                    :route-params {:id uid}
                                    :auth {:user-id 10}))
            mock-response (handler/update-file mock-request)]
        (is (= 400 (:status mock-response)))
        (is (= "Bad email address"
               (-> mock-response :body :message)))))))

(deftest uploaded-files-test
  (testing "should return body for not authenticated when no auth provided"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (is (= (handler/uploaded-files
              (mock/request :get "/api/user/files"))
             {:status 401
              :body {:success false
                     :message "Not authenticated"}}))))
  (testing "should return list of uploaded files for user"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (let [response (handler/uploaded-files
                      (assoc
                       (mock/request :get "/api/user/files")
                       :auth {}))]
        (is (= 200 (:status  response)))
        (is (-> response :body :success))
        (is (= (-> response :body :data count) 2))))))

(deftest get-shared-with-users-test
  (with-redefs [file-service/get-shared-user-details
                (constantly shared-with-user-details)
                file-service/is-file-owner? (constantly true)]
    (let [uid (str (UUID/randomUUID))
          mock-response (-> (mock/request :get
                                          (str "/api/user/files/" uid "/shared"))
                            (assoc :route-params {:id uid}
                                   :auth {:user-id 11})
                            (handler/get-shared-with-users))]
      (is (= 200 (:status mock-response)))
      (is (= (-> mock-response :body :data)
             shared-with-user-details)))))
