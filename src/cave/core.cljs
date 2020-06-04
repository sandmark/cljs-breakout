(ns cave.core)

(def config
  {:paddle-height      10
   :paddle-width       75
   :dx                 2  ;; Direction of Ball-x
   :dy                 -2 ;; Direction of Ball-y
   :ball-radius        10
   :right-pressed      false
   :left-pressed       false
   :brick-row-count    3
   :brick-column-count 5
   :brick-width        75
   :brick-height       20
   :brick-padding      10
   :brick-offset-top   30
   :brick-offset-left  30
   :score              0})

(defonce app-state (atom {}))

(def PI (.-PI js/Math))

(defn draw-ball []
  (let [{:keys [ctx x y ball-radius]} @app-state]
    (set! (.-fillStyle ctx) "#0095DD")
    (doto ctx
      (.beginPath)
      (.arc x y ball-radius 0 (* PI 2))
      (.fill)
      (.closePath))))

(defn draw-paddle []
  (let [{:keys [ctx height paddle-height paddle-width paddle-x]} @app-state]
    (set! (.-fillStyle ctx) "#0095DD")
    (doto ctx
      (.beginPath)
      (.rect paddle-x (- height paddle-height) paddle-width paddle-height)
      (.fill)
      (.closePath))))

(defn clear-timer []
  (js/clearInterval (:timer @app-state)))

(defn- game-over []
  (js/alert "GAME OVER")
  (.reload js/document.location)
  (clear-timer))

(defn- bound []
  (let [{:keys [x dx width ball-radius y dy height
                paddle-x paddle-width]} @app-state
        toggle                          #(swap! app-state update % -)]
    (when (or (> (+ x dx) (- width ball-radius))
              (< (+ x dx) ball-radius))
      (toggle :dx))

    (when (< (+ y dy) ball-radius)
      (toggle :dy))

    (when (> (+ y dy) (- height ball-radius))
      (if (and (> x paddle-x)
               (< x (+ paddle-x paddle-width)))
        (toggle :dy)
        (game-over)))))

(defn- move-ball []
  (doseq [[pos dir] [[:x :dx] [:y :dy]]]
    (swap! app-state update pos + (dir @app-state))))

(defn- handle-paddle []
  (let [{:keys [right-pressed left-pressed paddle-x width paddle-width]} @app-state]
    (cond (and right-pressed (< paddle-x (- width paddle-width)))
          (swap! app-state update :paddle-x + 7)

          (and left-pressed (pos? paddle-x))
          (swap! app-state update :paddle-x - 7))))

(defn draw-bricks []
  (let [{:keys [ctx brick-column-count brick-row-count
                brick-padding brick-width brick-height
                brick-offset-left brick-offset-top]} @app-state]
    (doseq [c (range brick-column-count)
            r (range brick-row-count)]
      (when (= 1 (get-in @app-state [:bricks r c :status]))
        (let [x (+ (* c (+ brick-width brick-padding)) brick-offset-left)
              y (+ (* r (+ brick-height brick-padding)) brick-offset-top)]
          (swap! app-state assoc-in [:bricks r c :x] x)
          (swap! app-state assoc-in [:bricks r c :y] y)
          (set! (.-fillStyle ctx) "#0095DD")
          (doto ctx
            (.beginPath)
            (.rect x y brick-width brick-height)
            (.fill)
            (.closePath)))))))

(defn- detect-game-win []
  (let [{:keys [score brick-row-count brick-column-count]} @app-state]
    (when (= score (* brick-row-count brick-column-count))
      (js/alert "YOU WIN, CONGRATULATIONS!")
      (clear-timer)
      (.reload js/document.location))))

(defn- detect-collision []
  (let [{:keys [brick-row-count brick-column-count
                brick-width brick-height x y
                bricks]} @app-state]
    (doseq [c (range brick-column-count)
            r (range brick-row-count)]
      (let [{bx :x by :y status :status} (get-in bricks [r c])]
        (when (= status 1)
          (when (and (> x bx)
                     (< x (+ bx brick-width))
                     (> y by)
                     (< y (+ by brick-height)))
            (swap! app-state update :dy -)
            (swap! app-state assoc-in [:bricks r c] 0)
            (swap! app-state update :score inc)
            (detect-game-win)))))))

(defn- draw-score []
  (let [{:keys [ctx score]} @app-state]
    (set! (.-font ctx) "16px Arial")
    (set! (.-fillStyle ctx) "#0095DD")
    (.fillText ctx (str "Score: " score) 8 20)))

(defn draw []
  (let [{:keys [ctx width height]} @app-state]
    (.clearRect ctx 0 0 width height)
    (draw-ball)
    (draw-paddle)
    (draw-bricks)
    (draw-score)

    (bound)

    (handle-paddle)
    (detect-collision)
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
  (let [update-pressed #(swap! app-state assoc % pressed?)]
    (fn [e]
      (case (.-key e)
        ("Right" "ArrowRight") (update-pressed :right-pressed)
        ("Left" "ArrowLeft")   (update-pressed :left-pressed)
        nil))))

(defn- make-bricks []
  (let [{:keys [brick-row-count
                brick-column-count]} config]
    {:bricks (->> (repeat brick-column-count {:x 0, :y 0, :status 1})
                  (into [])
                  (repeat brick-row-count)
                  (into []))}))

(defn start []
  (let [{:keys [width height] :as canvas} (make-canvas)
        ball                              (make-ball width height)
        paddle                            (make-paddle width)
        bricks                            (make-bricks)]
    (reset! app-state (merge config canvas ball paddle bricks))
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
  (clear-timer))
