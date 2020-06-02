(ns cave.core)

(defonce app-state (atom {:canvas nil
                          :ctx    nil
                          :x      nil
                          :y      nil
                          :dx     nil
                          :dy     nil}))

(defn draw-ball [{:keys [ctx x y]}]
  (set! (.-fillStyle ctx) "#0095DD")
  (doto ctx
    (.beginPath)
    (.arc x y 10 0 (* Math/PI 2))
    (.fill)
    (.closePath)))

(defn draw [state]
  (let [{:keys [canvas ctx dx dy]} @state
        width                      (.-width canvas)
        height                     (.-height canvas)]
    (.clearRect ctx 0 0 width height)
    (draw-ball @state)
    (swap! state update :x + dx)
    (swap! state update :y + dy)))

(defn start []
  (let [canvas (.getElementById js/document "app")
        system {:canvas canvas
                :ctx    (.getContext canvas "2d")
                :x      (/ (.-width canvas) 2)
                :y      (- (.-height canvas) 30)
                :dx     2
                :dy     -2}]
    (swap! app-state merge system)
    (js/setInterval #(draw app-state) 10)))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
