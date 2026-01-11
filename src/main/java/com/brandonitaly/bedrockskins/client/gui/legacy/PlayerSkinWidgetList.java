package com.brandonitaly.bedrockskins.client.gui.legacy;

import java.util.*;

public class PlayerSkinWidgetList {
    public final int x, y;
    public final List<PlayerSkinWidget> widgets;
    public int index;
    // Fields accessed by screen or logic
    public PlayerSkinWidget element3; // Center

    // Keeping other fields for potential debug/legacy access, though mostly internal to sortForIndex now
    public PlayerSkinWidget element0;
    public PlayerSkinWidget element1;
    public PlayerSkinWidget element2;
    public PlayerSkinWidget element4;
    public PlayerSkinWidget element5;
    public PlayerSkinWidget element6;

    // Constants
    private static final int VERTICAL_OFFSET = 10;
    private static final int OFFSET = 80;
    private static final float FACING_FROM_LEFT = -45f;
    private static final float FACING_FROM_RIGHT = 45f;

    private PlayerSkinWidgetList(int x, int y, PlayerSkinWidget[] widgets) {
        this.x = x;
        this.y = y;
        this.widgets = new ArrayList<>(Arrays.asList(widgets));
    }

    public static PlayerSkinWidgetList of(int x, int y, PlayerSkinWidget... widgets) {
        return new PlayerSkinWidgetList(x, y, widgets);
    }

    public void sortForIndex(int index) {
        if (widgets.isEmpty()) {
            this.index = 0;
            return;
        }

        // Loop the index properly
        int n = widgets.size();
        this.index = ((index % n) + n) % n;
        
        this.element3 = widgets.get(this.index);
        
        Set<PlayerSkinWidget> usedWidgets = new HashSet<>();
        
        // Priority list: Center, then Left (-1), then Right (1), expanding outwards.
        int[] offsets = {0, -1, 1, -2, 2, -3, 3, -4, 4};
        
        for (int offset : offsets) {
            PlayerSkinWidget w = getWrapped(this.index + offset);
            if (w == null) continue;
            
            if (usedWidgets.contains(w)) {
                continue; // Skip if already placed
            }
            
            usedWidgets.add(w);
            
            // Only reset pose for non-center elements. 
            if (offset != 0) {
                w.resetPose();
            }

            setupSlot(w, offset);
            
            // Assign to named fields
            if (offset == 0) this.element3 = w;
            else if (offset == -1) this.element2 = w;
            else if (offset == -2) this.element1 = w;
            else if (offset == -3) this.element0 = w;
            else if (offset == 1) this.element4 = w;
            else if (offset == 2) this.element5 = w;
            else if (offset == 3) this.element6 = w;
        }

        // Now we hide the widgets that were NOT used in this layout
        for (PlayerSkinWidget w : widgets) {
            if (!usedWidgets.contains(w)) {
                w.invisible();
                w.resetPose();
            }
        }
    }
    
    private PlayerSkinWidget getWrapped(int i) {
         if (widgets.isEmpty()) return null;
         int n = widgets.size();
         int wrapped = (i % n);
         if (wrapped < 0) wrapped += n;
         return widgets.get(wrapped);
    }

    private void setupSlot(PlayerSkinWidget w, int offset) {
        float rotX = 0;
        float rotY = 0;
        int targetPosX = x;
        int targetPosY = y;
        float scale = 1.0f;
        
        // Calculate target positions based on offset
        switch (offset) {
            case 0: // Center
                w.interactable = true;
                rotY = 0;
                targetPosX = x + 8;
                targetPosY = y + 20;
                scale = 0.85f;
                break;
            case -1: // Left 1
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET + 18;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
                break;
            case 1: // Right 1
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET + 20;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
                break;
            case -2: // Left 2
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET - 45;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
                break;
            case 2: // Right 2
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 2 + 18;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
                break;
            case -3: // Left 3
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 3;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
                break;
            case 3: // Right 3
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 3 + 35;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
                break;
            case -4: // Left 4
                w.interactable = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 4 + 80;
                targetPosY = y + VERTICAL_OFFSET + 10;
                scale = 0.4f;
                break;
            case 4: // Right 4
                w.interactable = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 4;
                targetPosY = y + VERTICAL_OFFSET * 4 + 20;
                scale = 0.4f;
                break;
            default:
                w.invisible();
                return;
        }
        
        // Wrap detection
        // Standard slot-to-slot move is ~80px. A wrap is usually > 120px.
        int currentX = w.getX();
        
        if (w.visible && Math.abs(currentX - targetPosX) > 120) {
            // It's a wrap. 
            int virtualTargetX;
            if (targetPosX > currentX) {
                // Moving Right (Wrapping Left->Right)
                // Simulate moving further Left
                virtualTargetX = currentX - OFFSET;
            } else {
                // Moving Left (Wrapping Right->Left)
                // Simulate moving further Right
                virtualTargetX = currentX + OFFSET;
            }
            
            w.visible();
            w.beginInterpolation(rotX, rotY, virtualTargetX, targetPosY, scale);
            // Snap to the REAL target after animation
            w.snapTo(targetPosX, targetPosY);
        } else {
            // Normal movement
            w.visible();
            w.beginInterpolation(rotX, rotY, targetPosX, targetPosY, scale);
        }
    }
}