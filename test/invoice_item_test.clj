(ns invoice-item-test
  (:require [invoice-item]))

(use 'clojure.test)


;; Note that I slightly modified the 'subtotal' method as I was having trouble to call it with the given signature
;; I hope this slightly change still allows me to show test coverage capabilities

;; Also, note that I've been running this with the following command 'clojure -X:test'

(deftest calculates-correct-subtotal
  (is (= (invoice-item/subtotal {:precise-quantity 10
                                 :precise-price 5
                                 :discount-rate 0}) 50.0))
  (is (= (invoice-item/subtotal {:precise-quantity 30
                                 :precise-price 1
                                 :discount-rate 0}) 30.0))
  (is (= (invoice-item/subtotal {:precise-quantity 30
                                 :precise-price 2.5
                                 :discount-rate 0}) 75.0)))


(deftest does-not-brake-with-missing-discount-rate
  (is (= (invoice-item/subtotal {:precise-quantity 2
                                 :precise-price 5}) 10.0))
  (is (= (invoice-item/subtotal {:precise-quantity 30
                                 :precise-price 30}) 900.0)))


(deftest applies-discount-rate
  (is (= (invoice-item/subtotal {:precise-quantity 2
                                 :precise-price 5
                                 :discount-rate 10}) 9.0))
  (is (= (invoice-item/subtotal {:precise-quantity 30
                                 :precise-price 30
                                 :discount-rate 10}) 810.0)))

(deftest returns-0-with-discount-rate-100
  (is (= (invoice-item/subtotal {:precise-quantity 2
                                 :precise-price 5
                                 :discount-rate 100}) 0.0))
  (is (= (invoice-item/subtotal {:precise-quantity 30
                                 :precise-price 30
                                 :discount-rate 100}) 0.0)))


(deftest throws-exception-if-quantity-is-negative
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-quantity -2
                                                           :precise-price 4}))))


(deftest throws-exception-if-price-is-negative
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-quantity 2
                                                           :precise-price -4}))))


(deftest throws-exception-if-quantity-is-missing
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-price 4}))))


(deftest throws-exception-if-price-is-missing
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-quantity 2}))))


(deftest throws-exception-with-negative-discount-rate
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-quantity 2
                                                           :precise-price 5
                                                           :discount-rate -100}))))

(deftest throws-exception-with-discount-rate-over-100
  (is (thrown? java.lang.Exception (invoice-item/subtotal {:precise-quantity 2
                                                           :precise-price 48
                                                           :discount-rate 1000}))))
