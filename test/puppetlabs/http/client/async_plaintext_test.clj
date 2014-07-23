(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client AsyncHttpClient RequestOptions)
           (org.apache.http.impl.nio.client HttpAsyncClients))
  (:require [clojure.test :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [req]
  {:status 200
   :body "Hello, World!"})

(tk/defservice test-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app "/hello")
        context))

(defn basic-test
  [http-method java-method clj-fn]
  (testing (format "async client: HTTP method: '%s'" http-method)
    (testlogging/with-test-logging
      (testutils/with-app-with-config app
        [jetty9/jetty9-service test-web-service]
        {:webserver {:port 10000}}
        (testing "java async client"
          (let [options (RequestOptions. "http://localhost:10000/hello/")
                response (java-method options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "clojure async client"
          (let [response (clj-fn "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))))))

(deftest async-client-head-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java async client"
        (let [options (RequestOptions. "http://localhost:10000/hello/")
              response (AsyncHttpClient/head options)]
          (is (= 200 (.getStatus (.deref response))))
          (is (= nil (.getBody (.deref response))))))
      (testing "clojure sync client"
        (let [response (async/head "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= nil (:body @response))))))))

(deftest async-client-get-test
  (basic-test "GET" #(AsyncHttpClient/get %) async/get))

(deftest async-client-post-test
  (basic-test "POST" #(AsyncHttpClient/post %) async/post))

(deftest async-client-put-test
  (basic-test "PUT" #(AsyncHttpClient/put %) async/put))

(deftest async-client-delete-test
  (basic-test "DELETE" #(AsyncHttpClient/delete %) async/delete))

(deftest async-client-trace-test
  (basic-test "TRACE" #(AsyncHttpClient/trace %) async/trace))

(deftest async-client-options-test
  (basic-test "OPTIONS" #(AsyncHttpClient/options %) async/options))

(deftest async-client-patch-test
  (basic-test "PATCH" #(AsyncHttpClient/patch %) async/patch))

(deftest persistent-async-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
    [jetty9/jetty9-service test-web-service]
    {:webserver {:port 10000}}
    (let [client (async/create-client {})]
      (testing "HEAD request with persistent async client"
        (let [response (common/head client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= nil (:body @response)))))
      (testing "GET request with persistent async client"
        (let [response (common/get client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "POST request with persistent async client"
        (let [response (common/post client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "PUT request with persistent async client"
        (let [response (common/put client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "DELETE request with persistent async client"
        (let [response (common/delete client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "TRACE request with persistent async client"
        (let [response (common/trace client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "OPTIONS request with persistent async client"
        (let [response (common/options client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "PATCH request with persistent async client"
        (let [response (common/patch client "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (slurp (:body @response))))))
      (testing "client closes properly"
        (common/close client)
        (is (thrown? IllegalStateException (common/get client "http://localhost:10000/hello/"))))))))

(deftest request-with-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (let [client (HttpAsyncClients/createDefault)
            opts   {:method :get :url "http://localhost:10000/hello/"}]
        (.start client)
        (testing "GET request works with request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "Client persists when passed to request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (.close client)))))