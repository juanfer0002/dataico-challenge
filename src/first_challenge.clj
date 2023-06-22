

(ns first-challenge
  (:require [clojure.edn :as edn]))

(def invoice (edn/read-string (slurp "invoice.edn")))


(defn has-iva [rate taxes]
  (->> taxes
       (filter #(= (:tax/category %) :iva))
       (filter #(= (:tax/rate %) rate))
       (not-empty)))


(defn has-retention [rate retentions]
  (->> retentions
       (filter #(= (:retention/category %) :ret_fuente))
       (filter #(= (:retention/rate %) rate))
       (not-empty)))


(defn has-iva-but-no-retf [iva retf item]
  (and (has-iva iva (:taxable/taxes item))
       (not (has-retention retf (:retentionable/retentions item)))))


(defn has-retf-but-no-iva [iva retf item]
  (and (has-retention retf (:retentionable/retentions item))
       (not (has-iva iva (:taxable/taxes item)))))


(defn filter-items-with-iva-but-no-retf [iva retf]
  (->> (invoice :invoice/items)
       (filter #(has-iva-but-no-retf iva retf %))))


(defn filter-items-with-retf-but-no-iva [iva retf]
  (->> (invoice :invoice/items)
       (filter #(has-retf-but-no-iva iva retf %))))


(defn filter-items-with-iva-xor-retf [iva retf]
  (->> (list (filter-items-with-iva-but-no-retf iva retf)
             (filter-items-with-retf-but-no-iva iva retf))
       (flatten)
       ;; I am just returning the id of the items to make it easier to identify the correct items 
       (map #(:invoice-item/id %))
       ;; if the idea is to get the list of maps, then it is enough to comment the line above
       ))

;; I've been running this with 'clojure -X first-challenge/run' from the command line
(defn run [opts]
  (println (filter-items-with-iva-xor-retf 19 1)))

