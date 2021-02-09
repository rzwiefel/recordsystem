(ns recordsystem.rest
  (:require [compojure.core :refer [defroutes GET POST routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [ring.middleware.params :refer [wrap-params]]
            [recordsystem.data :as data]
            [recordsystem.parse :as parse]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]))


(def sort->keyword
  {"email"     :email,
   "birthdate" :date-of-birth,
   "name"      :last-name,
   "color"     :color,
   "firstname" :first-name})

(defn- wrap-json
  [handler]
  (fn [request]
    (let [output (handler request)]
      {:body    (json/write-str (parse/convert-dates (:body output))),
       :status  200,
       :headers {"Content-Type" "application/json"}})))


(defn- query-records
  [sort dir]
  (if (not (contains? sort->keyword sort))
    (str "Could not find attribute: " sort)
    (data/query
      :sorts
      [(sort->keyword sort)
       (if (= dir "desc") :desc :asc)])))

(defn- parse-fn
  [type]
  (condp = type
    "text/csv" parse/parse-comma
    "text/psv" parse/parse-pipe
    "text/ssv" parse/parse-pipe))

(defn- handle-post
  [body type]
  (let [parsed ((parse-fn type) body)]
    (doseq [item parsed] (data/store! item))
    parsed))

(defroutes
  api-routes
  (wrap-json
    (routes
      (GET "/active" [] "System Active")
      (POST "/records"
            {body :body, type :content-type}
            (handle-post (slurp body) type))
      (GET "/records/:sort"
           [sort :as {params :params}]
           (query-records sort (get params "dir")))))
  (route/not-found "Not found"))

(def wrapped-routes
  (wrap-params api-routes))


(defn -main [] (ring/run-jetty #'routes {:port 1337, :join? true}))

(comment
  (ring/run-jetty #'wrapped-routes {:port 1337, :join? false}))