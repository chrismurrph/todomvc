(ns user
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :refer (pprint)]
    [clojure.stacktrace :refer (print-stack-trace)]
    [clojure.tools.namespace.repl :refer [disable-reload! refresh clear set-refresh-dirs]]
    [com.stuartsierra.component :as component]
    [figwheel-sidecar.repl-api :as ra]
    [taoensso.timbre :refer [info set-level!]]
    [todomvc.system :as system]
    [datomic.api :as d]
    ))

;;FIGWHEEL

(def figwheel-config
  {:figwheel-options {:css-dirs ["resources/public/css"]}
   :build-ids        ["dev" "support"]
   :all-builds       (figwheel-sidecar.repl/get-project-cljs-builds)})

(defn start-figwheel
  "Start Figwheel on the given builds, or defaults to build-ids in `figwheel-config`."
  ([]
   (let [props (System/getProperties)
         all-builds (->> figwheel-config :all-builds (mapv :id))]
     (start-figwheel (keys (select-keys props all-builds)))))
  ([build-ids]
   (let [default-build-ids (:build-ids figwheel-config)
         build-ids (if (empty? build-ids) default-build-ids build-ids)]
     (println "STARTING FIGWHEEL ON BUILDS: " build-ids)
     (ra/start-figwheel! (assoc figwheel-config :build-ids build-ids))
     (ra/cljs-repl))))

;;SERVER

(set-refresh-dirs "dev/server" "src/server" "specs/server")

(defonce system (atom nil))

(set-level! :info)

(defn init
  "Create a web server from configurations. Use `start` to start it."
  []
  (reset! system (system/make-system)))

(defn start "Start (an already initialized) web server." [] (swap! system component/start))
(defn stop "Stop the running web server." []
  (swap! system component/stop)
  (reset! system nil))

(defn go "Load the overall web server system and start it." []
  (init)
  (start))

(defn reset
  "Stop the web server, refresh all namespace source code from disk, then restart the web server."
  []
  (stop)
  (refresh :after 'user/go))

(def system-ns #{"db" "db.type" "db.install" "db.part"
                 "db.lang" "fressian" "db.unique" "db.excise"
                 "db.cardinality" "db.fn" "db.sys"
                 "entity" "confirmity" "datomic-toolbox"
                 "constraint" "db.alter" "db.bootstrap"})

;(def todo-db-uri "datomic:dev://localhost:4334/too")

(defn db-uri [db-name]
  (str "datomic:dev://localhost:4334/" db-name))

;;datomic:dev://{transactor-host}:{port}/*
(def any-db-uri "datomic:dev://localhost:4334/*")

(defn get-database-names []
  (d/get-database-names any-db-uri))

(defn show-entities [db-name]
  (let [conn (d/connect (db-uri db-name))
        entities (d/q '[:find ?e ?ident
                 :in $ ?system-ns
                 :where
                 [?e :db/ident ?ident]
                 [(namespace ?ident) ?ns]
                 [((comp not contains?) ?system-ns ?ns)]]
               (d/db conn) system-ns)
        _ (.release conn)]
    entities))



