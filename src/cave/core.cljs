(ns cave.core)

(def config
  {:paddle-height 10
   :paddle-width  75
   :dx            2  ;; Direction of Ball-x
   :dy            -2 ;; Direction of Ball-y
   :ball-radius   10
   :right-pressed false
   :left-pressed  false})

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

(defn- bound []
  (let [{:keys [x dx width ball-radius
                y dy height]} @app-state
        toggle                #(swap! app-state update % -)]
    (when (or (> (+ x dx) (- width ball-radius))
              (< (+ x dx) ball-radius))
      (toggle :dx))

    (when (< (+ y dy) ball-radius)
      (toggle :dy))

    (when (> (+ y dy) (- height ball-radius))
      (js/alert "GAME OVER")
      (.reload js/document.location)
      (js/clearInterval (:timer @app-state)))))

(defn- move-ball []
  (doseq [[pos dir] [[:x :dx] [:y :dy]]]
    (swap! app-state update pos + (dir @app-state))))

(defn- handle-paddle []
  (let [{:keys [right-pressed left-pressed paddle-x width paddle-width]} @app-state]
    (cond (and right-pressed (< paddle-x (- width paddle-width)))
          (swap! app-state update :paddle-x + 7)

          (and left-pressed (pos? paddle-x))
          (swap! app-state update :paddle-x - 7))))

(defn draw []
  (let [{:keys [ctx width height]} @app-state]
    (.clearRect ctx 0 0 width height)
    (draw-ball @app-state)
    (draw-paddle @app-state)

    (bound)

    (handle-paddle)

    (move-ball)))

(defn- make-canvas []
  (let [canvas (.getElementById js/document "app")]
    {:canvas canvas
     :ctx    (.getContext canvas "2d")
     :width  (.-width canvas)
     :height (.-height canvas)}))

(defn- make-ball [width height]
  {:x (/ width 2), :y (- height 30)})

(defn- make-paddle [width]
  (let [{:keys [paddle-width]} config]
    {:paddle-x (/ (- width paddle-width) 2)}))

(defn- make-key-handler [pressed?]
  (let [update-pressed #(swap! app-state update % (constantly pressed?))]
    (fn [e]
      (case (.-key e)
        ("Right" "ArrowRight") (update-pressed :right-pressed)
        ("Left" "ArrowLeft")   (update-pressed :left-pressed)
        nil))))

(defn start []
  (let [{:keys [width height] :as canvas} (make-canvas)
        ball                              (make-ball width height)
        paddle                            (make-paddle width)]
    (reset! app-state (merge config canvas ball paddle))
    (swap! app-state assoc :timer (js/setInterval draw 10))
    (.addEventListener js/document "keydown" (make-key-handler true) false)
    (.addEventListener js/document "keyup" (make-key-handler false) false)))

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
