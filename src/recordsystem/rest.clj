(ns recordsystem.rest
  (:require [compojure.core :refer [defroutes GET POST routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [ring.middleware.params :refer [wrap-params]]
            [recordsystem.data :as data]
            [recordsystem.parse :as parse]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is]]
            [clojure.string :as str]))


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
  "Queries for records and returns sorted by `sort`.
  `dir` can be provided for direction, defaults `asc`"
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
    "text/ssv" parse/parse-space
    (fn [& args] (str "Could not find parser for: " type))))

(defn- handle-post
  "Parses `body` according to `type`"
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


(deftest
  test-parse-fn
  (is (= parse/parse-comma (parse-fn "text/csv")))
  (is (= parse/parse-pipe (parse-fn "text/psv")))
  (is (= parse/parse-space (parse-fn "text/ssv")))
  (is (str/includes? ((parse-fn "notamimetype") nil)
                     "notamimetype")))


(deftest
  test-query
  (data/remove-all!)
  (data/store! {:email "minnie@disney.com",
                :last-name "mouse",
                :color :pink,
                :first-name "minnie",
                :date-of-birth "11/18/1928"})
  (data/store! {:email "mickey@disney.com",
                :last-name "mouse",
                :color :blue,
                :first-name "mickey",
                :date-of-birth "12/05/1901"})
  (is (= '("minnie" "mickey") (map :first-name (query-records "birthdate" nil))))
  (is (= '("mickey" "minnie") (map :first-name (query-records "birthdate" "desc"))))
  (data/remove-all!))

