(ns app
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let}}}
  (:require-macros [hiccups.core :as h])
  (:require [hiccups.runtime]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.pprint :refer [cl-format]]
            [promesa.core :as p]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mimic DB (in-memory)
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def todos (atom (sorted-map 1 {:id 1 :name "Taste htmx with ClojureScript & Deno" :done true}
                             2 {:id 2 :name "Buy a unicorn" :done false})))

(def todos-id (atom (count @todos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; "DB" queries
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-todo! [name]
  (let [id (swap! todos-id inc)]
    (swap! todos assoc id {:id id :name name :done false})))

(defn toggle-todo! [id]
  (swap! todos update-in [(js/Number id) :done] not))

(defn remove-todo! [id]
  (swap! todos dissoc (js/Number id)))

(defn filtered-todo [filter-name todos]
  (case filter-name
    "active" (remove #(:done (val %)) todos)
    "completed" (filter #(:done (val %)) todos)
    "all" todos
    todos))

(defn get-items-left []
  (count (remove #(:done (val %)) @todos)))

(defn todos-completed []
  (count (filter #(:done (val %)) @todos)))

(defn remove-all-completed-todo []
  (reset! todos (into {} (remove #(:done (val %)) @todos))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Template and components
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn todo-item [{:keys [id name done]}]
  [:li {:id (str "todo-" id)
        :class (when done "completed")}
   [:div.view
    [:input.toggle {:hx-patch (str "/todos/" id)
                    :type "checkbox"
                    :checked done
                    :hx-target (str "#todo-" id)
                    :hx-swap "outerHTML"}]
    [:label {:hx-get (str "/todos/edit/" id)
             :hx-target (str "#todo-" id)
             :hx-swap "outerHTML"} name]
    [:button.destroy {:hx-delete (str "/todos/" id)
                      :_ (str "on htmx:afterOnLoad remove #todo-" id)}]]])

(defn todo-list [todos]
  (for [todo todos]
    (todo-item (val todo))))

(defn todo-edit [id name]
  [:form {:hx-post (str "/todos/update/" id)}
   [:input.edit {:type "text"
                 :name "todo"
                 :value name}]])

(defn item-count []
  (let [items-left (get-items-left)]
    [:span#todo-count.todo-count {:hx-swap-oob "true"}
     [:strong items-left] (cl-format nil " item~p " items-left) "left"]))

(defn todo-filters [filter]
  [:ul.filters
   [:li
    [:a {:hx-get "/?filter=all"
         :hx-target "#todo-list"
         :class (when (= filter "all") "selected")} "All"]
    [:a {:hx-get "/?filter=active"
         :hx-target "#todo-list"
         :class (when (= filter "active") "selected")} "Active"]
    [:a {:hx-get "/?filter=completed"
         :hx-target "#todo-list"
         :class (when (= filter "completed") "selected")} "Completed"]]])

(defn clear-completed-button []
  [:button#clear-completed
   {:hx-delete "/todos"
    :hx-target "#todo-list"
    :hx-swap-oob "true"
    :class (str "clear-completed " (when-not (pos? (todos-completed)) "hidden"))}
   "Clear completed"])

(defn template [filter]
  (str
   "<!DOCTYPE html>"
   (h/html
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "htmx + ClojureScript + Deno"]
     [:link {:href "https://unpkg.com/todomvc-app-css@2.4.1/index.css" :rel "stylesheet"}]
     [:script {:src "https://unpkg.com/htmx.org@1.5.0/dist/htmx.min.js" :defer true}]
     [:script {:src "https://unpkg.com/hyperscript.org@0.8.1/dist/_hyperscript.min.js" :defer true}]]
    [:body
     [:section.todoapp
      [:header.header
       [:h1 "todos"]
       [:form
        {:hx-post "/todos"
         :hx-target "#todo-list"
         :hx-swap "beforeend"
         :_ "on htmx:afterOnLoad set #txtTodo.value to ''"}
        [:input#txtTodo.new-todo
         {:name "todo"
          :placeholder "What needs to be done?"
          :autofocus ""}]]]
      [:section.main
       [:input#toggle-all.toggle-all {:type "checkbox"}]
       [:label {:for "toggle-all"} "Mark all as complete"]]
      [:ul#todo-list.todo-list
       (todo-list (filtered-todo filter @todos))]
      [:footer.footer
       (item-count)
       (todo-filters filter)
       (clear-completed-button)]]
     [:footer.info
      [:p "Click to edit a todo"]
      [:p "Created by "
       [:a {:href "https://github.com/huygn"
            :target "_blank"
            :rel "noopener noreferrer"} "huygn"]]
      [:p "Part of "
       [:a {:href "http://todomvc.com"} "TodoMVC"]]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-body [req]
  (p/let [entries (-> (.formData req)
                      (.then #(.entries %)))]
    (-> (js/Object.fromEntries entries)
        (js->clj :keywordize-keys true))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn app-index [{:keys [filter]}]
  (if filter
    (h/html (todo-list (filtered-todo filter @todos)))
    (template filter)))

(defn add-item [req]
  (p/let [name (-> (parse-body req) (.then #(:todo %)))
          todo (add-todo! name)]
    (h/html (todo-item (val (last todo)))
            (item-count))))

(defn edit-item [id]
  (let [{:keys [id name]} (get @todos (js/Number id))]
    (h/html (todo-edit id name))))

(defn update-item [req id]
  (p/let [name (-> (parse-body req) (.then #(:todo %)))
          todo (swap! todos assoc-in [(js/Number id) :name] name)]
    (h/html (todo-item (get todo (js/Number id))))))

(defn patch-item [id]
  (let [todo (toggle-todo! id)]
    (h/html (todo-item (get todo (js/Number id)))
            (item-count)
            (clear-completed-button))))

(defn delete-item [id]
  (remove-todo! id)
  (h/html (item-count)))

(defn clear-completed []
  (remove-all-completed-todo)
  (h/html (todo-list)
          (item-count)
          (clear-completed-button)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [{:keys [method pathname query req]}]
  (p/let [path (vec (rest (str/split pathname #"/")))
          {:keys [body status] :or {status 200}}
          (match [method path]
            ["GET" []] {:body (app-index query)}
            ["GET" ["todos" "edit" id]] {:body (edit-item id)}
            ["POST" ["todos"]] {:body (add-item req)}
            ["POST" ["todos" "update" id]] {:body (update-item req id)}
            ["PATCH" ["todos" id]] {:body (patch-item id)}
            ["DELETE" ["todos" id]] {:body (delete-item id)}
            ["DELETE" ["todos"]] {:body (clear-completed)}
            :else {:status 404 :body "Error 404: Page not found"})
          resolved-body body]
    {:body resolved-body
     :status status}))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [req]
  (let [url (js/URL. (.-url req))
        query (-> (.-searchParams url)
                  (.entries)
                  (js/Object.fromEntries)
                  (js->clj :keywordize-keys true))]
    (routes {:method (.-method req)
             :pathname (.-pathname url)
             :query query
             :req req})))

(defn js-handler [req]
  (p/let [{:keys [body status]} (handler req)]
    #js{:body body
        :status status}))

(defn init []
  (js/addEventListener
   "fetch"
   (fn [e]
     (p/let [{:keys [body status]} (handler (.-request e))]
       (->> (js/Response. body
                          (clj->js {:status status
                                    :headers
                                    {:content-type "text/html;charset=utf-8"}}))
            (.respondWith e))))))
