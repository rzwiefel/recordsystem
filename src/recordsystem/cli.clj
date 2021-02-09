(ns recordsystem.cli
  (:require [recordsystem.parse :as parse]
            [recordsystem.data :as data]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer [deftest is run-all-tests]])
  (:import (java.io File)))

(defn- file-ext
  [path]
  (cond
    (nil? path)                  nil
    (str/ends-with? path ".csv") :csv
    (str/ends-with? path ".ssv") :ssv
    (str/ends-with? path ".psv") :psv
    :else                        (throw (IllegalArgumentException.
                                          "No match for that extension"))))

(defn- get-file
  [^String path]
  (let [f (File. path)]
    (try
      {:data (slurp f),
       :type (file-ext path),
       :path path,
       :file f}
      (catch Exception
        e
        {:path      path,
         :exception (.getMessage e)}))))

(defn- resolve-files
  [file-paths]
  (let [{errs true, data false} (group-by
                                  #(contains? % :exception)
                                  (map get-file file-paths))]
    [data errs]))

(defn- parse
  [item]
  (condp = (:type item)
    :csv (parse/parse-comma (:data item))
    :ssv (parse/parse-space (:data item))
    :psv (parse/parse-pipe (:data item))))

(defn -help
  [& args]
  (println "USAGE:")
  (println "  clojure -X:cli :input '[\"FILE1\" \"FILE2\" ..]'"))

(defn -main
  [{:keys [input output disable-date-format], :or {}, :as all}]
  (let [[input-files errors] (resolve-files input)]
    (if (seq errors)
      (println "Errors:" errors)
      (let [data   (flatten (map parse input-files))
            _      (doseq [item data] (data/store! item))
            sorted (data/query :sorts output)
            xform  (if disable-date-format identity parse/convert-dates)]
        (pprint (xform sorted))
        (xform sorted)))))


(deftest parsing-extension-works
  (is (= :csv (file-ext "abcdefg.csv")))
  (is (= :ssv (file-ext "abcdefg.ssv")))
  (is (= :psv (file-ext "abcdefg.psv")))
  (is (= nil (file-ext nil)))
  (is (thrown? IllegalArgumentException (file-ext "noextension"))))

(deftest get-file-works
  (is (contains? (get-file "abcdefgh") :exception))
  (let [temp (File/createTempFile "recordsystem.cli" ".psv")
        path (.getCanonicalPath temp)
        data "a | b | z@b.c | red | 1/1/2000"]
    (.deleteOnExit temp)
    (spit temp data)
    (let [res (get-file path)]
      (is (= data (:data res)))
      (is (= :psv (:type res))))
    (.delete temp)))

(defn run-tests [arg]
  (run-all-tests #"^recordsystem.*"))

