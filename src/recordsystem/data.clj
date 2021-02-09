(ns recordsystem.data
  (:require [clojure.test :refer [deftest is are use-fixtures]]))

(def db (atom []))
(def failed-parsing (atom []))




(defn store!
  "Store an item.
  TODO(RCZ) - Do we want to allow a collection to be passed as well?"
  [person]
  #_(println "data/store! receiving: " person)
  (swap! db #(conj % person)))

(defn store-failed-parsing! [item] (swap! failed-parsing #(conj % item)))

(defn- filter-db
  [queries data]
  (if (empty? queries)
    data
    (recur (rest queries) (filter (first queries) data))))

(defn- compare-by
  [sorts]
  (fn [x y]
    (loop [sorts sorts]
      (let [[[key-fn dir] & remain] sorts
            result                  (if (= :asc dir)
                                      (compare (key-fn x) (key-fn y))
                                      (compare (key-fn y) (key-fn x)))]
        (if (and (= result 0) (seq remain)) (recur remain) result)))))

(defn- sorted-by
  [sorts data]
  (if (empty? sorts)
    data
    (let [sorts (partition 2 sorts)] (sort (compare-by sorts) data))))

(defn query
  [& {:keys [filters sorts]}]
  "Query the database with optional list of :filters and/or :sorts.
  :filters is a list of predicates to apply to the result set. eg:
    [#(= :green (:color %))]
  :sorts is a list of attribute and directions to sort by, eg:
    [:email :desc :last-name :desc]"
  (->> @db
       (filter-db filters)
       (sorted-by sorts)))


(def addy
  {:first-name    "adalicia",
   :last-name     "zwiefelhofer",
   :email         "adalicia.zwiefelhofer@gmail.com",
   :color         :blue,
   :date-of-birth #inst "2019-01-10T07:00:00.000-00:00"})
(def ryan
  {:first-name    "ryan",
   :last-name     "zwiefelhofer",
   :email         "ryan.zwiefelhofer@gmail.com",
   :color         :blue,
   :date-of-birth #inst "1992-11-23T07:00:00.000-00:00"})
(def alyssa
  {:first-name    "alyssa",
   :last-name     "zwiefelhofer",
   :email         "agzwiefelhofer@gmail.com",
   :color         :green,
   :date-of-birth  #inst"1993-07-19T07:00:00.000-00:00"})
(def bryan
  {:first-name    "bryan",
   :last-name     "clonezwiefs",
   :email         "ryan.zwiefelhofer@gmail.com",
   :color         :blue,
   :date-of-birth #inst "1992-11-23T07:00:00.000-00:00"})

(defn db-test-fixture [f] (reset! db []) (f))
(use-fixtures :each db-test-fixture)

(deftest test-empty-db
  (println "test1")
  (is (empty? (query)))
  (is (empty? (query :filters [#(= :green (:color %))] :sorts [:email :desc]))))


(deftest sorting
  (println "test2")
  (reset! db [addy ryan alyssa bryan])
  #_(map store! [addy ryan alyssa bryan])
  (is (= (map :first-name [bryan ryan alyssa addy])
         (map :first-name (query :sorts [:email :desc :last-name :asc]))))
  (is (= (map :first-name [addy alyssa bryan ryan])
         (map :first-name (query :sorts [:first-name :asc])))))



(comment (map store! [addy ryan alyssa bryan])
         ((compare-by [[:email :desc]]) ryan addy)
         (query)
         (query :filters [#(= :green (:color %))])
         (query :sorts [:email :desc :last-name :asc])
         '())