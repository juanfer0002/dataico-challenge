(ns invoice-spec
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [clojure.set :as set]
   [clojure.data.json :as json]))

(defn not-blank? [value] (-> value clojure.string/blank? not))
(defn non-empty-string? [x] (and (string? x) (not-blank? x)))

(s/def :customer/name non-empty-string?)
(s/def :customer/email non-empty-string?)
(s/def :invoice/customer (s/keys :req [:customer/name
                                       :customer/email]))

(s/def :tax/rate double?)
(s/def :tax/category #{:iva})
(s/def ::tax (s/keys :req [:tax/category
                           :tax/rate]))
(s/def :invoice-item/taxes (s/coll-of ::tax :kind vector? :min-count 1))

(s/def :invoice-item/price double?)
(s/def :invoice-item/quantity double?)
(s/def :invoice-item/sku non-empty-string?)

(s/def ::invoice-item
  (s/keys :req [:invoice-item/price
                :invoice-item/quantity
                :invoice-item/sku
                :invoice-item/taxes]))

(s/def :invoice/issue-date inst?)
(s/def :invoice/items (s/coll-of ::invoice-item :kind vector?))

(s/def :invoice/name non-empty-string?)

(s/def ::invoice
  (s/keys :req [:invoice/name
                :invoice/issue-date
                :invoice/customer
                :invoice/items]))


(def date-formatter (java.text.SimpleDateFormat. "dd/MM/yyyy"))
(def read-json (get (json/read-str (slurp "invoice.json")) "invoice"))


(defn base-and-rename-keyname [keyname, base]
  (->> (string/replace keyname "_" "-")
       (str base "/")
       (keyword)))


(defn build-renaming-kmap [keynames base]
  (->> keynames
       (map #(vector % (base-and-rename-keyname % base)))
       (into {})))


(defn transform-keys-with-base [json base]
  (set/rename-keys json (build-renaming-kmap (keys json) base)))


(defn parse-str-to-date [str-date]
  (.parse date-formatter str-date))


(defn parse-issue-date [json]
  (-> json
      (update :invoice/issue-date #(parse-str-to-date %))))


(defn transform-nested-customer [json]
  (-> json
      (update :invoice/customer
              (fn [customer]
                (-> customer
                    (transform-keys-with-base "customer")
                    (select-keys [:customer/name
                                  :customer/email]))))))


(defn update-tax-rate-to-double [tax]
  (-> tax
      (update :tax/rate double)))

(defn update-tax-category-to-keyword [tax]
  (-> tax
      (update :tax/category string/lower-case)
      (update :tax/category keyword)))


(defn transform-nested-taxes [json]
  (-> json
      (update :invoice-item/taxes
              (fn [taxes]
                (->> taxes
                     (map #(-> % (transform-keys-with-base "tax")
                               (select-keys [:tax/rate
                                             :tax/category])))
                     (map #(update-tax-rate-to-double %))
                     (map #(update-tax-category-to-keyword %))
                     (into []))))))


(defn transform-nested-items [json]
  (-> json
      (update :invoice/items
              (fn [items]
                (->> items
                     (map #(-> % (transform-keys-with-base "invoice-item")
                               (select-keys [:invoice-item/price
                                             :invoice-item/quantity
                                             :invoice-item/sku
                                             :invoice-item/taxes])))
                     (map #(transform-nested-taxes %))
                     (into []))))))


(def invoice
  (-> read-json
      (transform-keys-with-base "invoice")
      (parse-issue-date)
      (transform-nested-customer)
      (transform-nested-items)
      (select-keys [:invoice/name
                    :invoice/issue-date
                    :invoice/customer
                    :invoice/items])))

;; A couple of notes
;; 1. I changed some of the json's attributes' names. To be precise, I removed the 'tax_' portion from the list of taxes just to make the code a bit clearer.
;;    Something similarly I did with the company name in the customer's object
;;    If this is not allowed, I am happy to revert it and handle the case, but I thought it might have been a typo.
;; 2. As I am not sure how to run this properly, I've been using the command line
;;    I've been running this with 'clojure -X invoice-spec/validate'
(defn validate [opts]
  (println (s/valid? ::invoice invoice)))

