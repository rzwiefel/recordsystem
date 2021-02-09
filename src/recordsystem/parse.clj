(ns recordsystem.parse
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is are use-fixtures]])
  (:import (java.util.regex Pattern)))

;The pipe-delimited file lists each record as follows:
; LastName | FirstName | Email | FavoriteColor | DateOfBirth
; The comma-delimited file looks like this:
; LastName, FirstName, Email, FavoriteColor, DateOfBirth
; The space-delimited file looks like this:
; LastName FirstName Email FavoriteColor DateOfBirth

(def date-format (java.text.SimpleDateFormat. "MM/dd/yyyy"))

(defn parse-date [s] (.parse date-format s))

(defn convert-dates
  "If data is a collection (but not a map), and contains maps
  then transform all dates to `date-format"
  [data]
  (if (and (coll? data) (not (map? data)) (map? (first data)))
    (for [item data]
      (apply
        hash-map
        (flatten (for [[k v] item]
                   (if (instance? java.util.Date v)
                     [k (.format date-format v)]
                     [k v])))))
    data))


(defn blank-or
  "Provides nil-punning for nil and empty string, or calls func"
  [func]
  (fn [^String arg]
    (if (or (nil? arg) (= "" arg))
      nil
      (func arg))))

(def record-types
  {:first-name    (blank-or (comp str/lower-case str)),
   :last-name     (blank-or (comp str/lower-case str)),
   :email         (blank-or (comp str/lower-case str)),
   :color         (blank-or (comp keyword str/lower-case)),
   :date-of-birth (blank-or parse-date)})

(defn- convert-types
  [data]
  (apply hash-map (flatten (for [[k v] data] [k ((k record-types) v)]))))

; TODO (RCZ) - Failure case? Return failure?
(defn- parse-infix
  "Returns a function that parses a string delimited by `delimiter` and
  reads data according to `order` listed"
  [^Pattern delimiter order]
  (fn [data]
    (let [lines       (str/split-lines data)
          parsed      (for [line lines]
                        (map str/trim (str/split line delimiter)))
          #_{:keys [valid invalid], :or {valid [], invalid []}}
          #_(group-by #(if (= (count order) (count %)) :valid :invalid) parsed)
          parsed      (filter #(= (count order) (count %)) parsed)
          unconverted (map #(partition 2 (interleave order %)) parsed)]
      (map convert-types unconverted))))


(def parse-pipe
  (parse-infix #"\|" [:last-name :first-name :email :color :date-of-birth]))
(def parse-comma
  (parse-infix #"," [:last-name :first-name :email :color :date-of-birth]))
(def parse-space
  (parse-infix #"\s" [:last-name :first-name :email :color :date-of-birth]))


(deftest missing-fields-behvior
  (are [parse-fn data expected]
       (let [parsed (parse-fn data)] (= (first parsed) expected))
       parse-pipe
       "z |  | rz@gmail.com | blue | 1/23/1992"
       {:last-name     "z",
        :first-name    nil,
        :email         "rz@gmail.com",
        :color         :blue,
        :date-of-birth #inst "1992-01-23T07:00:00.000-00:00"}
       parse-pipe
       "z | r | rz@gmail.com | | 1/23/1992"
       {:last-name     "z",
        :first-name    "r",
        :email         "rz@gmail.com",
        :color         nil,
        :date-of-birth #inst "1992-01-23T07:00:00.000-00:00"}
       parse-pipe
       "z | r | rz@gmail.com | orange | "
       {:last-name     "z",
        :first-name    "r",
        :email         "rz@gmail.com",
        :color         :orange,
        :date-of-birth nil}))


(deftest invalid-data-ignored
  (are [parse-fn num-valid data]
       (let [parsed (parse-fn data)] (= num-valid (count parsed)))
       parse-pipe
       0
       "doe | john jd@email.com | blue | 1/23/1992"

       parse-pipe
       0
       "!@$(&%!@*#&!@*&!@%*(#(\naaaaaaaaaaaaaaa"

       parse-pipe
       1
       "!@#$%^&*()-=\na|a|a@a.a|red|1/1/2000"))

(deftest parsing-works
  (let
    [pipe-data
       "Zwiefelhofer | ryan | ryan.zwie@gmail.com | blue | 1/23/1992
Badahdah | chris | a.badh@gmail.com | red | 04/15/1992
Hickey | rich | rich.hickey@cognitect.com | green | 07/27/1967"
     parsed    (parse-pipe pipe-data)]
    (is (= 3 (count parsed)))
    (is (= #{:blue :red :green} (set (map :color parsed))))
    (is (= #{"ryan" "chris" "rich"} (set (map :first-name parsed)))))
  (let
    [comma-data
       "Zwiefelhofer, ryan, ryan.zwie@gmail.com, blue, 1/23/1992
Badahdah, chris, a.bad@gmail.com, red, 04/15/1992
Hickey, rich, rich.hickey@cognitect.com, green, 07/27/1967"
     parsed     (parse-comma comma-data)]
    (is (= 3 (count parsed)))
    (is (= #{:blue :red :green}) (set (map :color parsed)))
    (is (= #{"ryan" "chris" "rich"} (set (map :first-name parsed)))))
  (let
    [space-data
       "Zwiefelhofer ryan ryan.zwie@gmail.com blue 1/23/1992
Badahdah chris a.bah@gmail.com red 04/15/1992
Hickey rich rich.hickey@cognitect.com green 07/27/1967"
     parsed     (parse-space space-data)]
    (is (= 3 (count parsed)))
    (is (= #{:blue :red :green}) (set (map :color parsed)))
    (is (= #{"ryan" "chris" "rich"} (set (map :first-name parsed))))))