(ns cave.core)

(def config
  {:paddle-height 10
   :paddle-width  75
   :dx            2  ;; Direction of Ball-x
   :dy            -2 ;; Direction of Ball-y
   :ball-radius   10})

(defonce app-state (atom {}))

(def PI (.-PI js/Math))

(defn draw-ball [{:keys [ctx x y ball-radius]}]
  (set! (.-fillStyle ctx) "#0095DD")
  (doto ctx
    (.beginPath)
    (.arc x y ball-radius 0 (* PI 2))
    (.fill)
    (.closePath)))

(defn draw-paddle [{:keys [ctx height paddle-height paddle-width paddle-x]}]
  (set! (.-fillStyle ctx) "#0095DD")
  (doto ctx
    (.beginPath)
    (.rect paddle-x (- height paddle-height) paddle-width paddle-height)
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

(defn- move-ball []
  (doseq [[pos dir] [[:x :dx] [:y :dy]]]
    (swap! app-state update pos + (dir @app-state))))

(defn draw []
  (let [{:keys [ctx width height]} @app-state]
    (.clearRect ctx 0 0 width height)
    (draw-ball @app-state)
    (draw-paddle @app-state)

    (bound [:dx :dy])

    (move-ball)))

(defn- canvas []
  (let [canvas (.getElementById js/document "app")]
    {:canvas canvas
     :ctx    (.getContext canvas "2d")
     :width  (.-width canvas)
     :height (.-height canvas)}))

(defn start []
  (let [{:keys [width height] :as params} (canvas)
        positions                         {:x        (/ width 2)
                                           :y        (- height 30)
                                           :paddle-x (/ (- width (:paddle-width config)) 2)}]
    (reset! app-state (merge config params positions))
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
