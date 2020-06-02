(ns cave.core)

(defonce app-state (atom {:dx          2  ;; Direction of Ball-x
                          :dy          -2 ;; Direction of Ball-y
                          :ball-radius 10
                          }))

(def PI (.-PI js/Math))

(defn draw-ball [{:keys [ctx x y ball-radius]}]
  (set! (.-fillStyle ctx) "#0095DD")
  (doto ctx
    (.beginPath)
    (.arc x y ball-radius 0 (* PI 2))
    (.fill)
    (.closePath)))

(defn- out-of-border? [ball-radius pos direction border]
  (or (> (+ pos direction) (- border ball-radius))
      (< (+ pos direction) ball-radius)))

(let [kmap {:dx [:ball-radius :x :dx :width]
            :dy [:ball-radius :y :dy :height]}]
  (defn- bound [ks]
    (doseq [k ks]
      (let [params (map (partial get @app-state) (k kmap))]
        (when (apply out-of-border? params)
          (swap! app-state update k -))))))

(defn draw []
  (let [{:keys [ctx dx dy width height]} @app-state]
    (.clearRect ctx 0 0 width height)
    (draw-ball @app-state)

    (bound [:dx :dy])

    (swap! app-state update :x + dx)
    (swap! app-state update :y + dy)))

(defn- canvas []
  (let [canvas (.getElementById js/document "app")]
    {:canvas canvas
     :ctx    (.getContext canvas "2d")
     :width  (.-width canvas)
     :height (.-height canvas)}))

(defn start []
  (let [{:keys [width height] :as params} (canvas)
        pos                               {:x (/ width 2)
                                           :y (- height 30)}]
    (reset! app-state (merge params pos))
    (swap! app-state assoc :timer (js/setInterval draw 10))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop")
  (js/clearInterval (:timer @app-state)))
