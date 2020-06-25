(ns receive.handlers.api
  (:require
   [receive.error-handler :refer [if-error
                                  error->http-response]]
   [receive.handlers.helper :refer [map-response-data]]
   [receive.service.files :as files]
   [receive.service.user :as user]))

(defn success [data]
  {:status 200
   :body {:success true
          :data data}})

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn sign-in [request]
  (let [id-token (-> request :params :id_token)
        token (user/signin-with-google id-token)]
    (if-error token
              :http-response
              {:status 200
               :cookies {"access_token" {:value token
                                 ;; TODO: set :secure true after HTTPS is enabled
                                         :http-only true
                                         :same-site :strict
                                         :path "/"}}
               :body {:data token
                      :success true
                      :message "User authenticated"}})))

(defn fetch-user [{auth :auth}]
  (if auth
    (success (user/get-user (:user_id auth)))
    (error->http-response {:error :unauthorized})))

(defn update-file [{:keys [params route-params auth]}]
  (let [result
        (files/find-and-update-file auth
                                    (:id route-params)
                                    {:private? (:is_private params)
                                     :shared-with-user-emails (:shared_with_users params)})]
    (if-error result
              :http-response
              {:status 200
               :body {:success true
                      :data result}})))

(defn get-shared-with-users [{:keys [route-params auth]}]
  (let [uid (:id route-params)
        is-owner? (files/is-file-owner? auth uid)]
    (if is-owner?
      (let [result (files/get-shared-user-details (:id route-params))]
        (if-error result
                  :http-response
                  {:status 200
                   :body {:success true
                          :data result}}))
      (error->http-response {:error :forbidden}))))

(defn uploaded-files [{auth :auth}]
  (if auth
    (success (->> (:user_id auth)
                  (files/get-uploaded-files)
                  (map (map-response-data :filename
                                          :uid
                                          :created_at))))
    (error->http-response {:error :unauthorized})))
