(ns recordsystem.rest
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [clojure.edn :as edn]))



(defroutes
  routes
  (GET "/" [] "<h1>HI</h1>")
  (route/not-found "<h1>Page not found</h1>"))



(defn -main [] (ring/run-jetty #'routes {:port 1337, :join? true}))

(comment
  (ring/run-jetty #'routes {:port 1337, :join? false}))