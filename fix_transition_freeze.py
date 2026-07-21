import re

file_path = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\AdminViewNew.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

old_method = """    private void playTransitionAndGo(String videoFilename, Runnable action) {
        // Disable UI interaction
        view.setMouseTransparent(true);

        // Hide UI elements smoothly but don't play it yet
        FadeTransition hideUi = new FadeTransition(Duration.millis(300), shellContainer);
        hideUi.setToValue(0.0);

        try {
            if (currentTransitionPlayer != null) {
                currentTransitionPlayer.stop();
                currentTransitionPlayer.dispose();
                currentTransitionPlayer = null;
            }

            String videoUrl = getClass().getResource("/styles/assets/videos/" + videoFilename).toExternalForm();
            Media media = new Media(videoUrl);
            MediaPlayer player = new MediaPlayer(media);
            currentTransitionPlayer = player;
            
            mediaView.setMediaPlayer(player);
            mediaView.setVisible(true);
            
            player.setOnEndOfMedia(() -> {
                action.run();
                view.setMouseTransparent(false);
                // We keep the media view visible or not depending on the action,
                // but usually the action replaces the root scene anyway.
            });
            
            player.setOnError(() -> {
                System.err.println("Media player error: " + player.getError());
                action.run();
            });

            player.setOnReady(() -> {
                hideUi.play();
                player.play();
            });

        } catch (Exception e) {
            System.err.println("Error playing video transition: " + e.getMessage());
            action.run();
        }
    }"""

new_method = """    private void playTransitionAndGo(String videoFilename, Runnable action) {
        // Disable UI interaction
        view.setMouseTransparent(true);

        // Hide UI elements smoothly but don't play it yet
        FadeTransition hideUi = new FadeTransition(Duration.millis(300), shellContainer);
        hideUi.setToValue(0.0);

        // Add a hard fallback timer of 4.5 seconds to guarantee the action runs 
        // even if the MediaPlayer hangs or fails to reach EndOfMedia.
        javafx.animation.PauseTransition fallbackTimer = new javafx.animation.PauseTransition(Duration.millis(4500));
        
        final boolean[] actionRun = {false};
        Runnable safeAction = () -> {
            if (!actionRun[0]) {
                actionRun[0] = true;
                if (currentTransitionPlayer != null) {
                    currentTransitionPlayer.stop();
                }
                action.run();
                view.setMouseTransparent(false);
            }
        };

        fallbackTimer.setOnFinished(ev -> {
            if (!actionRun[0]) {
                System.out.println("Fallback timer triggered for " + videoFilename + " - forcing transition");
                safeAction.run();
            }
        });
        fallbackTimer.play();

        try {
            if (currentTransitionPlayer != null) {
                currentTransitionPlayer.stop();
                currentTransitionPlayer.dispose();
                currentTransitionPlayer = null;
            }

            String videoUrl = getClass().getResource("/styles/assets/videos/" + videoFilename).toExternalForm();
            Media media = new Media(videoUrl);
            MediaPlayer player = new MediaPlayer(media);
            currentTransitionPlayer = player;
            
            mediaView.setMediaPlayer(player);
            mediaView.setVisible(true);
            
            player.setOnEndOfMedia(() -> {
                safeAction.run();
            });
            
            player.setOnError(() -> {
                System.err.println("Media player error: " + player.getError());
                safeAction.run();
            });

            player.setOnReady(() -> {
                hideUi.play();
                player.play();
                
                // Dynamically adjust fallback to the video duration + 1 second if possible
                javafx.util.Duration mediaDuration = player.getMedia().getDuration();
                if (mediaDuration.isFinite()) {
                    fallbackTimer.setDuration(mediaDuration.add(javafx.util.Duration.millis(1000)));
                    fallbackTimer.playFromStart();
                }
            });

        } catch (Exception e) {
            System.err.println("Error playing video transition: " + e.getMessage());
            safeAction.run();
        }
    }"""

content = content.replace(old_method, new_method)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Fixed media player freeze logic")
