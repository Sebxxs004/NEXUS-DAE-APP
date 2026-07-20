package com.prisma.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

public class ResponsiveUtils {

    /**
     * Creates a responsive scene by wrapping the original view in a letterboxing container.
     * The original view will be scaled proportionally to fit the screen size.
     * 
     * @param view       The root pane of the view designed for a specific resolution.
     * @param baseWidth  The width the view was designed for (e.g., 1500).
     * @param baseHeight The height the view was designed for (e.g., 900).
     * @return A new Scene that automatically scales its contents.
     */
    public static Scene createResponsiveScene(Parent view, double baseWidth, double baseHeight) {
        // Enforce the base size on the original view
        if (view instanceof Region) {
            Region region = (Region) view;
            region.setPrefSize(baseWidth, baseHeight);
            region.setMinSize(baseWidth, baseHeight);
            region.setMaxSize(baseWidth, baseHeight);
        }

        // Group ignores layout bounds and allows its children to be scaled freely
        Group scalingGroup = new Group(view);

        // StackPane acts as the letterboxing container (black bars on sides if aspect ratio differs)
        StackPane wrapper = new StackPane(scalingGroup);
        wrapper.setStyle("-fx-background-color: #04091A;"); // Dark background for letterbox areas

        Scene scene = new Scene(wrapper);

        // Bind the scale of the group to the minimum ratio between the actual screen size and the base size
        Scale scale = new Scale();
        NumberBinding scaleBinding = Bindings.min(
                wrapper.widthProperty().divide(baseWidth),
                wrapper.heightProperty().divide(baseHeight)
        );

        scale.xProperty().bind(scaleBinding);
        scale.yProperty().bind(scaleBinding);
        
        // Pivot point at 0,0 is standard for Group scaling when centered by StackPane
        scale.setPivotX(0);
        scale.setPivotY(0);

        scalingGroup.getTransforms().add(scale);

        return scene;
    }
}
