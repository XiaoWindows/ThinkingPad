import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.Timer;

import java.awt.geom.RoundRectangle2D;

// 动画常量定义 - 无帧率限制极致性能模式
class AnimationConstants {
    // 无帧率限制：使用最小时间间隔，最大化刷新率
    public static final int MIN_FRAME_INTERVAL = 1; // ~1000+FPS（系统决定实际上限）

    // 检测是否启用高性能模式（仅保留判断，不影响帧率）
    public static boolean isHighPerformanceMode() {
        int deviceType = GraphicsEnvironment.getLocalGraphicsEnvironment()
                           .getDefaultScreenDevice().getType();
        return deviceType == java.awt.GraphicsDevice.TYPE_RASTER_SCREEN;
    }

    // 记录帧渲染时间（空实现）
    public static void recordFrameRender() {
        // 无性能监控，零开销
    }

    // 获取无限制的最优帧间隔 - 始终最小间隔
    public static int getOptimalFrameInterval() {
        return MIN_FRAME_INTERVAL;
    }

    // 获取当前性能指标（固定返回无限制）
    public static String getPerformanceInfo() {
        return "无帧率限制极致性能模式";
    }

    // 手动设置目标帧率（禁用）
    public static void setTargetFps(int fps) {
        System.out.println("无帧率限制模式：已禁用帧率设置，运行在系统最大性能");
    }
}

    // 黄金比例布局管理器（横向版本 - 左右布局）
class GoldenRatioLayout implements LayoutManager {
    public static final String LEFT = "Left";
    public static final String RIGHT = "Right";
    
    private int gap;
    private Component leftComponent;
    private Component rightComponent;
    
    public GoldenRatioLayout(int gap) {
        this.gap = gap;
    }
    
    @Override
    public void addLayoutComponent(String name, Component comp) {
        if (LEFT.equals(name)) {
            leftComponent = comp;
        } else if (RIGHT.equals(name)) {
            rightComponent = comp;
        }
    }
    
    @Override
    public void removeLayoutComponent(Component comp) {
        if (comp == leftComponent) {
            leftComponent = null;
        } else if (comp == rightComponent) {
            rightComponent = null;
        }
    }
    
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return minimumLayoutSize(parent);
    }
    
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        int width = 0;
        int height = 0;
        
        if (leftComponent != null) {
            Dimension d = leftComponent.getMinimumSize();
            width += d.width;
            height = Math.max(height, d.height);
        }
        
        if (rightComponent != null) {
            Dimension d = rightComponent.getMinimumSize();
            width += d.width;
            height = Math.max(height, d.height);
        }
        
        width += gap;
        return new Dimension(width, height);
    }
    
    @Override
    public void layoutContainer(Container parent) {
        int width = parent.getWidth();
        int height = parent.getHeight();
        
        // 黄金比例：左侧38.2%，右侧61.8%
        double goldenRatio = 0.382;
        int leftWidth = (int)((width - gap) * goldenRatio);
        int rightWidth = width - leftWidth - gap;
        
        // 布局左侧组件
        if (leftComponent != null) {
            leftComponent.setBounds(0, 0, leftWidth, height);
        }
        
        // 布局右侧组件
        if (rightComponent != null) {
            rightComponent.setBounds(leftWidth + gap, 0, rightWidth, height);
        }
    }
    
    // 获取左侧组件
    public Component getLeftComponent() {
        return leftComponent;
    }
    
    // 获取右侧组件
    public Component getRightComponent() {
        return rightComponent;
    }
}

    // 黄金比例布局管理器（竖向版本 - 上大下小）
class VerticalGoldenRatioLayout implements LayoutManager {
    public static final String TOP = "Top";
    public static final String BOTTOM = "Bottom";
    
    private int gap;
    private Component topComponent;
    private Component bottomComponent;
    
    public VerticalGoldenRatioLayout(int gap) {
        this.gap = gap;
    }
    
    @Override
    public void addLayoutComponent(String name, Component comp) {
        if (TOP.equals(name)) {
            topComponent = comp;
        } else if (BOTTOM.equals(name)) {
            bottomComponent = comp;
        }
    }
    
    @Override
    public void removeLayoutComponent(Component comp) {
        if (comp == topComponent) {
            topComponent = null;
        } else if (comp == bottomComponent) {
            bottomComponent = null;
        }
    }
    
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return minimumLayoutSize(parent);
    }
    
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        int width = 0;
        int height = 0;
        
        if (topComponent != null) {
            Dimension d = topComponent.getMinimumSize();
            width = Math.max(width, d.width);
            height += d.height;
        }
        
        if (bottomComponent != null) {
            Dimension d = bottomComponent.getMinimumSize();
            width = Math.max(width, d.width);
            height += d.height;
        }
        
        height += gap;
        return new Dimension(width, height);
    }
    
    @Override
    public void layoutContainer(Container parent) {
        int width = parent.getWidth();
        int height = parent.getHeight();
        
        // 黄金比例：上部分38.2%，下部分61.8%（上小下大）
        double goldenRatio = 0.382;
        int topHeight = (int)((height - gap) * goldenRatio);
        int bottomHeight = height - topHeight - gap;
        
        // 布局上部分组件（问题显示框）
        if (topComponent != null) {
            topComponent.setBounds(0, 0, width, topHeight);
        }
        
        // 布局下部分组件（文本输入框）
        if (bottomComponent != null) {
            bottomComponent.setBounds(0, topHeight + gap, width, bottomHeight);
        }
    }
}

// 动态背景面板
class DynamicBackgroundPanel extends JPanel {
    private java.awt.Image backgroundImage;
    private java.awt.Image blurredBackground;
    private java.awt.Image nextBackgroundImage;
    private java.awt.Image nextBlurredBackground;
    private boolean imageLoaded = false;
    private Timer animationTimer;
    private ArrayList<java.awt.Image> backgroundImages = new ArrayList<>();
    private ArrayList<java.awt.Image> blurredImages = new ArrayList<>();
    private int currentImageIndex = 0;
    private float backgroundTransition = 0.0f;
    private boolean isTransitioning = false;
    private Timer transitionTimer;
    
    // 性能优化：缓存缩放后的背景图
    private java.awt.Image cachedScaledBackground;
    private java.awt.Image cachedScaledBlurredBackground;
    private int lastPanelWidth = -1;
    private int lastPanelHeight = -1;
    
    public DynamicBackgroundPanel() {
        setOpaque(true);
        loadBackgroundImage();
        startAnimation();
    }
    
    private void loadBackgroundImage() {
        try {
            // 创建image文件夹（如果不存在）
            String imageDirPath = System.getProperty("user.dir") + File.separator + "image";
            File imageDir = new File(imageDirPath);
            if (!imageDir.exists()) {
                imageDir.mkdirs();
                System.out.println("创建image文件夹: " + imageDir.getAbsolutePath());
            }
            
            // 搜索image文件夹中的所有图片文件
            String[] supportedFormats = {"jpg", "jpeg", "png", "gif", "bmp"};
            ArrayList<String> imageFiles = new ArrayList<>();
            
            for (String format : supportedFormats) {
                File[] files = imageDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith("." + format.toLowerCase()));
                if (files != null) {
                    for (File file : files) {
                        imageFiles.add(file.getAbsolutePath());
                    }
                }
            }
            
            // 如果image文件夹没有图片，回退到原来的路径
            if (imageFiles.isEmpty()) {
                String[] fallbackPaths = {
                    "Thinking.jpg",
                    "e:/Java程序/随机问题/Thinking.jpeg", 
                    "e:\\\\Java程序\\\\随机问题\\\\Thinking.jpeg",
                    System.getProperty("user.dir") + "/Thinking.jpeg",
                    System.getProperty("user.dir") + "\\\\Thinking.jpeg"
                };
                
                for (String path : fallbackPaths) {
                    File imageFile = new File(path);
                    if (imageFile.exists() && imageFile.isFile()) {
                        imageFiles.add(imageFile.getAbsolutePath());
                    }
                }
            }
            
            // 加载找到的所有图片
            for (String imagePath : imageFiles) {
                try {
                    java.awt.Image image = javax.imageio.ImageIO.read(new File(imagePath));
                    if (image != null) {
                        backgroundImages.add(image);
                        blurredImages.add(createGaussianBlur(image));
                        System.out.println("背景图片加载成功: " + imagePath);
                    }
                } catch (Exception e) {
                    System.out.println("加载图片失败: " + imagePath + " - " + e.getMessage());
                }
            }
            
            if (!backgroundImages.isEmpty()) {
                // 设置第一张图片为当前背景
                backgroundImage = backgroundImages.get(0);
                blurredBackground = blurredImages.get(0);
                currentImageIndex = 0;
                imageLoaded = true;
                System.out.println("总共加载了 " + backgroundImages.size() + " 张背景图片");
            } else {
                System.out.println("未找到背景图片，使用默认动态背景");
                System.out.println("当前工作目录: " + System.getProperty("user.dir"));
                createDynamicBackground();
            }
        } catch (Exception e) {
            System.out.println("加载背景图片失败: " + e.getMessage());
            createDynamicBackground();
        }
    }
    
    private java.awt.Image createGaussianBlur(java.awt.Image originalImage) {
        try {
            // 性能优化：简化模糊处理，使用预处理的低质量模糊
            // 避免运行时卷积运算造成的性能瓶颈
            BufferedImage original = new BufferedImage(
                originalImage.getWidth(null), 
                originalImage.getHeight(null), 
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = original.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, null);
            g.dispose();
            
            // 使用更简单的模糊：降低图像质量来模拟模糊效果
            // 这比卷积运算快得多，适合实时动画
            BufferedImage blurred = new BufferedImage(
                original.getWidth(), original.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D bg = blurred.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            bg.drawImage(original, 0, 0, original.getWidth(), original.getHeight(), null);
            bg.dispose();
            
            return blurred;
        } catch (Exception e) {
            return originalImage;
        }
    }
    
    private void createDynamicBackground() {
        int width = 1920;
        int height = 1080;
        backgroundImage = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) backgroundImage.getGraphics();
        
        // 创建渐变背景
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(15, 23, 42), 
            width, height, new Color(30, 41, 59)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
        
        // 添加网格效果
        g2d.setColor(new Color(59, 130, 246, 20));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < width; i += 50) {
            g2d.drawLine(i, 0, i, height);
        }
        for (int i = 0; i < height; i += 50) {
            g2d.drawLine(0, i, width, i);
        }
        
        g2d.dispose();
        imageLoaded = true;
        blurredBackground = backgroundImage;
    }
    



    
    private void startAnimation() {
        // 修复：避免重复创建Timer，重用现有Timer
        if (animationTimer != null && animationTimer.isRunning()) {
            return; // Timer已经在运行，不需要重新创建
        }
        
        // 如果Timer存在但没有运行，先停止它
        if (animationTimer != null) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            repaint();
        });
        animationTimer.start();
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }

    }
    
    // 切换到下一张背景图片
    public void switchToNextBackground() {
        // 如果没有图片或只有一张图片，不切换
        if (backgroundImages.size() <= 1) {
            return;
        }
        
        // 如果正在切换中，忽略这次请求
        if (isTransitioning) {
            return;
        }
        
        // 计算下一张图片的索引
        int nextIndex = (currentImageIndex + 1) % backgroundImages.size();
        
        // 如果只有一张图片，不切换
        if (nextIndex == currentImageIndex) {
            return;
        }
        
        // 设置下一张图片
        nextBackgroundImage = backgroundImages.get(nextIndex);
        nextBlurredBackground = blurredImages.get(nextIndex);
        
        // 开始切换动画
        startBackgroundTransition(nextIndex);
    }
    
    private void startBackgroundTransition(int nextIndex) {
        isTransitioning = true;
        backgroundTransition = 0.0f;
        
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }
        
        transitionTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            backgroundTransition += 0.08f; // 进一步加快背景切换速度
            
            if (backgroundTransition >= 1.0f) {
                backgroundTransition = 1.0f;
                
                // 完成切换
                backgroundImage = nextBackgroundImage;
                blurredBackground = nextBlurredBackground;
                currentImageIndex = nextIndex;
                nextBackgroundImage = null;
                nextBlurredBackground = null;
                isTransitioning = false;
                
                transitionTimer.stop();
            }
            
            repaint();
        });
        transitionTimer.start();
    }
    

    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED); // 改为速度优先
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        
        int width = getWidth();
        int height = getHeight();
        
        // 性能优化：只在必要时重绘背景
        if (blurredBackground != null) {
            drawScaledImage(g2d, blurredBackground, width, height, 1.0f);
        }
        
        // 背景切换时使用更高效的混合模式
        if (isTransitioning && nextBlurredBackground != null) {
            // 使用硬件加速的混合模式替代软件透明度计算
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, backgroundTransition));
            drawScaledImage(g2d, nextBlurredBackground, width, height, 1.0f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        

        
    }
    
    private void drawScaledImage(Graphics2D g2d, java.awt.Image image, int panelWidth, int panelHeight, float alpha) {
        if (image == null) return;
        
        // 性能优化：缓存缩放后的图像，避免每帧重复计算
        java.awt.Image targetImage = image;
        String cacheKey = panelWidth + "x" + panelHeight;
        
        if (panelWidth == lastPanelWidth && panelHeight == lastPanelHeight) {
            // 使用缓存的背景图
            if (image == backgroundImage && cachedScaledBackground != null) {
                targetImage = cachedScaledBackground;
            } else if (image == blurredBackground && cachedScaledBlurredBackground != null) {
                targetImage = cachedScaledBlurredBackground;
            }
        } else {
            // 尺寸改变，更新缓存
            lastPanelWidth = panelWidth;
            lastPanelHeight = panelHeight;
            
            if (image == backgroundImage) {
                cachedScaledBackground = createScaledInstance(image, panelWidth, panelHeight);
                targetImage = cachedScaledBackground;
            } else if (image == blurredBackground) {
                cachedScaledBlurredBackground = createScaledInstance(image, panelWidth, panelHeight);
                targetImage = cachedScaledBlurredBackground;
            }
        }
        
        // 直接绘制，避免重复的尺寸计算
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        
        // 性能优化：使用更快的绘制方式
        g2d.drawImage(targetImage, 0, 0, panelWidth, panelHeight, null);
        
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    // 高性能图像缩放方法
    private java.awt.Image createScaledInstance(java.awt.Image image, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return scaled;
    }
    

}

// 自定义圆角按钮
class ModernButton extends JButton {
    private Color backgroundColor;
    private Color hoverColor;
    private Color currentColor;
    private float colorTransition = 0.0f;
    private Timer animationTimer;
    private boolean isHovering = false;
    private Timer hoverTimer;
    private float hoverAnimation = 0.0f;
    private float clickAnimation = 0.0f;
    private Timer clickTimer;

    
    public ModernButton(String text, Color bgColor, Color hovColor) {
        super(text);
        this.backgroundColor = bgColor;
        this.hoverColor = hovColor;
        this.currentColor = bgColor;
        
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("微软雅黑", Font.BOLD, 14));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 鼠标事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                startHoverAnimation();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                startHoverAnimation();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startClickAnimation();
            }
        });
        
        startAnimation();
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (isHovering && hoverAnimation < 1.0f) {
                hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!isHovering && hoverAnimation > 0.0f) {
                hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }
    
    private void startClickAnimation() {
        clickAnimation = 1.0f;
        
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
        
        clickTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            clickAnimation = Math.max(0, clickAnimation - 0.08f);
            repaint();
            
            if (clickAnimation <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        clickTimer.start();
    }
    

    

    

    
    private void startAnimation() {
        // 波纹动画已移除
    }
    

    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // 绘制圆角裁剪区域
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, 15, 15));
        
        // 单层玻璃效果：半透明背景
        g2d.setColor(new Color(255, 255, 255, 25));
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 重置裁剪区域
        g2d.setClip(null);
        

        
        // 绘制点击动画效果（颜色变深）
        if (clickAnimation > 0) {
            g2d.setColor(new Color(0, 0, 0, (int)(clickAnimation * 40)));
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
        }
        
        // 绘制边框（常显边框，与文本输入框一致）
        int borderAlpha = 80; // 固定透明度，与文本输入框一致
        g2d.setColor(new Color(255, 255, 255, borderAlpha));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        
        // 悬停时添加额外的边框效果
        if (isHovering) {
            int hoverBorderAlpha = (int)(hoverAnimation * 120);
            g2d.setColor(new Color(255, 255, 255, hoverBorderAlpha));
            g2d.setStroke(new BasicStroke(1.5f + hoverAnimation * 0.5f));
            g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        }
        
        // 绘制文字
        g2d.setColor(getForeground());
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(getText(), textX, textY);
        
        g2d.dispose();
    }
}

// 自定义圆角文本区域
class ModernTextArea extends JTextArea {
    private boolean isHovering = false;
    private boolean hasFocus = false;
    private float hoverAnimation = 0.0f;
    private float focusAnimation = 0.0f;
    private Timer hoverTimer;
    private Timer focusTimer;
    private float clickAnimation = 0.0f;
    private Timer clickTimer;
    
    public ModernTextArea() {
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new Font("微软雅黑", Font.BOLD, 15)); // 加粗字体
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setCursor(new Cursor(Cursor.TEXT_CURSOR));
        setEnabled(true);
        setEditable(true);
        
        // 鼠标事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                startHoverAnimation();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                startHoverAnimation();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startClickAnimation();
            }
        });
        
        // 焦点事件
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                hasFocus = true;
                startFocusAnimation();
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                hasFocus = false;
                startFocusAnimation();
            }
        });
        

    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (isHovering && hoverAnimation < 1.0f) {
                hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!isHovering && hoverAnimation > 0.0f) {
                hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }
    
    private void startFocusAnimation() {
        if (focusTimer != null && focusTimer.isRunning()) {
            focusTimer.stop();
        }
        
        focusTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (hasFocus && focusAnimation < 1.0f) {
                focusAnimation = Math.min(focusAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!hasFocus && focusAnimation > 0.0f) {
                focusAnimation = Math.max(focusAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        focusTimer.start();
    }
    
    public void cleanup() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (focusTimer != null && focusTimer.isRunning()) {
            focusTimer.stop();
        }
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
    }
    
    private void startClickAnimation() {
        clickAnimation = 1.0f;
        
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
        
        clickTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            clickAnimation = Math.max(0, clickAnimation - 0.08f);
            repaint();
            
            if (clickAnimation <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        clickTimer.start();
    }
    

    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // 绘制圆角裁剪区域
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, 15, 15));
        
        // 单层玻璃效果：半透明背景
        g2d.setColor(new Color(255, 255, 255, 25));
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 重置裁剪区域
        g2d.setClip(null);
        
        // 边框效果（常显边框）
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        
        // 悬停时添加额外的边框效果
        if (isHovering) {
            int hoverBorderAlpha = (int)(hoverAnimation * 100);
            g2d.setColor(new Color(255, 255, 255, hoverBorderAlpha));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        }
        
        // 绘制点击动画效果（颜色变深）
        if (clickAnimation > 0) {
            g2d.setColor(new Color(0, 0, 0, (int)(clickAnimation * 30)));
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
        }
        
        // 焦点效果已移除
        
        // 设置文本区域的裁剪区域，确保文本不超出圆角边界
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(2, 2, width-4, height-4, 13, 13));
        
        // 调用父类方法在裁剪区域内绘制文本内容
        super.paintComponent(g2d);
        
        g2d.dispose();
    }
    

}

// 通知组件类
class NotificationPanel extends JPanel {
    private String message;
    private String title;
    private NotificationType type;
    private Timer animationTimer;
    private Timer hideTimer;
    private Timer backgroundDimTimer;
    private float animationProgress = 0.0f;
    private boolean isShowing = false;
    private boolean isHiding = false;
    private int targetX;
    private int startX;
    private int targetY;
    private int startY;
    private float currentAlpha = 0.0f;
    private float backgroundDimAlpha = 0.0f;
    private JPanel backgroundDimPanel;
    
    public enum NotificationType {
        SUCCESS(new Color(76, 175, 80)),
        ERROR(new Color(244, 67, 54)),
        WARNING(new Color(255, 152, 0)),
        INFO(new Color(33, 150, 243));
        
        private final Color color;
        NotificationType(Color color) {
            this.color = color;
        }
        
        public Color getColor() {
            return color;
        }
    }
    
    public NotificationPanel(String title, String message, NotificationType type) {
        this.title = title;
        this.message = message;
        this.type = type;
        setOpaque(false);
        setLayout(new BorderLayout());
        
        // 根据文本内容计算合适的大小
        calculatePreferredSize();
        
        // 使用更合适的边距
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        // 创建内容面板，使用GridBagLayout进行更精确的布局控制
        JPanel contentPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 去掉深色背景，只保留玻璃效果
                // g2d.setColor(new Color(0, 0, 0, (int)(80 * currentAlpha))); // 更深的黑色背景
                // g2d.fillRoundRect(0, 0, width, height, 12, 12);
                
                // 绘制细白色边框
                g2d.setColor(new Color(255, 255, 255, (int)(100 * currentAlpha)));
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 11, 11);
                
                super.paintComponent(g);
                g2d.dispose();
            }
        };
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 类型指示器
        JPanel typeIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制圆形类型指示器
                g2d.setColor(type.getColor());
                g2d.fillOval(2, 2, 8, 8);
                
                g2d.dispose();
            }
        };
        typeIndicator.setOpaque(false);
        typeIndicator.setPreferredSize(new Dimension(12, 12));
        
        // 标题标签 - 性能优化：移除透明度合成，在父级统一处理
        JLabel titleLabel = new JLabel(title) {
            @Override
            public void paint(Graphics g) {
                // 性能优化：直接在父级处理透明度，避免重复合成
                super.paint(g);
            }
        };
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        titleLabel.setForeground(new Color(255, 255, 255));
        
        // 消息标签 - 性能优化：移除透明度合成
        JTextArea messageArea = new JTextArea(message) {
            @Override
            public void paint(Graphics g) {
                // 性能优化：透明度在父级统一处理
                super.paint(g);
            }
        };
        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageArea.setForeground(new Color(255, 255, 255));
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // 左右增加5像素边距，避免文本顶在最左边
        
        // 布局标题行
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 0, 8, 8);
        contentPanel.add(typeIndicator, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 0, 8, 0);
        contentPanel.add(titleLabel, gbc);
        
        // 布局消息行
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // 跨越两列
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(messageArea, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * 根据文本内容计算合适的首选大小
     */
    private void calculatePreferredSize() {
        // 创建临时组件来测量文本尺寸
        JLabel tempTitle = new JLabel(title);
        tempTitle.setFont(new Font("微软雅黑", Font.BOLD, 13));
        
        JTextArea tempMessage = new JTextArea(message);
        tempMessage.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        tempMessage.setWrapStyleWord(true);
        tempMessage.setLineWrap(true);
        
        // 计算标题宽度和高度
        FontMetrics titleMetrics = tempTitle.getFontMetrics(tempTitle.getFont());
        int titleWidth = titleMetrics.stringWidth(title) + 20; // 加上类型指示器的空间
        int titleHeight = titleMetrics.getHeight();
        
        // 计算消息尺寸，支持中英文混合
        FontMetrics messageMetrics = tempMessage.getFontMetrics(tempMessage.getFont());
        
        // 设置消息的合理宽度范围
        int minWidth = Math.max(250, titleWidth + 30); // 最小宽度要能容纳标题
        int maxWidth = 380; // 最大宽度避免过宽
        int optimalWidth = minWidth;
        
        // 计算在最优宽度下的消息行数
        int[] lineInfo = calculateTextLines(message, messageMetrics, optimalWidth);
        int lineCount = lineInfo[0];
        int actualWidth = lineInfo[1];
        
        // 如果行数过多，适当增加宽度
        if (lineCount > 3) {
            optimalWidth = Math.min(maxWidth, optimalWidth + 50);
            lineInfo = calculateTextLines(message, messageMetrics, optimalWidth);
            lineCount = lineInfo[0];
            actualWidth = lineInfo[1];
        }
        
        int messageHeight = lineCount * messageMetrics.getHeight();
        
        // 计算总尺寸，加上边距和间距
        int totalWidth = Math.max(titleWidth + 30, actualWidth + 40); // 左右边距
        int totalHeight = titleHeight + messageHeight + 40; // 标题+消息+间距+边距
        
        // 设置最小和最大尺寸限制
        totalWidth = Math.max(minWidth, Math.min(maxWidth, totalWidth));
        totalHeight = Math.max(70, Math.min(180, totalHeight));
        
        setPreferredSize(new Dimension(totalWidth, totalHeight));
    }
    
    /**
     * 计算文本在指定宽度下的行数
     */
    private int[] calculateTextLines(String text, FontMetrics metrics, int maxWidth) {
        if (text == null || text.trim().isEmpty()) {
            return new int[]{0, 0};
        }
        
        // 按字符和空格分割文本
        String[] words = text.split("\\s+");
        int currentLineLength = 0;
        int lineCount = 1;
        int maxLineLength = 0;
        
        for (String word : words) {
            int wordLength = metrics.stringWidth(word + " ");
            
            // 如果单个词就超过最大宽度，强制换行
            if (wordLength > maxWidth - 20) {
                if (currentLineLength > 0) {
                    lineCount++;
                    currentLineLength = 0;
                }
                // 处理超长词
                int charLength = metrics.stringWidth(word.substring(0, 1));
                int charsPerLine = Math.max(1, (maxWidth - 20) / charLength);
                int extraLines = (word.length() + charsPerLine - 1) / charsPerLine;
                lineCount += extraLines - 1;
                currentLineLength = metrics.stringWidth(word.substring(word.length() - (word.length() % charsPerLine))) + wordLength;
            } else if (currentLineLength + wordLength <= maxWidth - 20) {
                currentLineLength += wordLength;
            } else {
                lineCount++;
                currentLineLength = wordLength;
            }
            
            maxLineLength = Math.max(maxLineLength, currentLineLength);
        }
        
        return new int[]{lineCount, maxLineLength};
    }
    
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 设置整体透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));
        
        // 调用父类绘制
        super.paint(g2d);
        g2d.dispose();
    }
    
    public void showNotification() {
        if (isShowing || isHiding) return;
        
        isShowing = true;
        animationProgress = 0.0f;
        currentAlpha = 0.0f;
        // 去掉背景渐暗效果
        // backgroundDimAlpha = 0.0f;
        
        // 从左侧进入的动画设置
        int panelWidth = getParent() != null ? getParent().getWidth() : 900;
        startX = -getWidth(); // 从窗口左侧外部开始
        targetX = 0; // 目标位置完全紧贴左边边缘（x=0）
        
        startY = getLocation().y;
        targetY = getLocation().y;
        
        setLocation(startX, startY);
        
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            AnimationConstants.recordFrameRender(); // 记录帧渲染性能
            
            // 调快动画速度：提高动画步进值
            animationProgress += 0.08f;
            
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                isShowing = false;
                animationTimer.stop();
                
                // 3秒后自动隐藏
                hideTimer = new Timer(3000, evt -> hideNotification());
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
            
            // 使用缓动函数
            float easedProgress = easeOutCubic(animationProgress);
            currentAlpha = easedProgress;
            
            // 水平移动（从左到右）
            int currentX = startX + (int)((targetX - startX) * easedProgress);
            int currentY = startY;
            
            setLocation(currentX, currentY);
            
            // 性能优化：只在透明度变化时重绘
            if (easedProgress > 0) {
                repaint();
            }
        });
        animationTimer.start();
    }
    
    public void hideNotification() {
        if (isShowing || isHiding) return;
        
        isHiding = true;
        animationProgress = 0.0f;
        
        // 向左侧退出的动画设置
        startX = getLocation().x;
        targetX = -getWidth(); // 退出到窗口左侧外部
        
        startY = getLocation().y;
        targetY = getLocation().y;
        
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            // 调快退出动画速度
            animationProgress += 0.1f;
            
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                isHiding = false;
                animationTimer.stop();
                
                // 从父容器中移除
                Container parent = getParent();
                if (parent != null) {
                    parent.remove(this);
                    parent.revalidate();
                    // 性能优化：延迟重绘，减少重复渲染
                    SwingUtilities.invokeLater(parent::repaint);
                }
                return;
            }
            
            // 使用缓动函数
            float easedProgress = easeInCubic(animationProgress);
            currentAlpha = 1.0f - easedProgress;
            
            // 水平移动（从右到左退出）
            int currentX = startX + (int)((targetX - startX) * easedProgress);
            int currentY = startY;
            
            setLocation(currentX, currentY);
            
            // 性能优化：减少重绘频率
            if (easedProgress > 0) {
                repaint();
            }
        });
        animationTimer.start();
    }
    
    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }
    
    private float easeInCubic(float t) {
        return t * t * t;
    }
    
    private void createBackgroundDimPanel() {
        Container parent = getParent();
        if (parent == null) return;
        
        // 创建背景渐暗面板
        backgroundDimPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制半透明黑色遮罩
                g2d.setColor(new Color(0, 0, 0, (int)(backgroundDimAlpha * 255)));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                g2d.dispose();
            }
        };
        backgroundDimPanel.setOpaque(false);
        backgroundDimPanel.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        
        // 将渐暗面板添加到父容器中（在通知面板之前）
        parent.add(backgroundDimPanel, JLayeredPane.POPUP_LAYER, 0);
        parent.revalidate();
        parent.repaint();
    }
    
    private void removeBackgroundDimPanel() {
        if (backgroundDimPanel != null) {
            Container parent = backgroundDimPanel.getParent();
            if (parent != null) {
                parent.remove(backgroundDimPanel);
                parent.revalidate();
                parent.repaint();
            }
            backgroundDimPanel = null;
        }
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (hideTimer != null && hideTimer.isRunning()) {
            hideTimer.stop();
        }
        if (backgroundDimTimer != null && backgroundDimTimer.isRunning()) {
            backgroundDimTimer.stop();
        }
        removeBackgroundDimPanel();
    }
}

// 动画问题标签
class AnimatedQuestionLabel extends JLabel {
    private String currentText;
    private String newText;
    private float animationProgress = 0.0f;
    private boolean isAnimating = false;
    private Timer animationTimer;
    private boolean isHovering = false;
    private float hoverAnimation = 0.0f;
    private Timer hoverTimer;
    private float clickAnimation = 0.0f;
    private Timer clickTimer;
    private Runnable onRefreshCallback;
    
    public AnimatedQuestionLabel(String text) {
        super(text);
        this.currentText = text;
        this.newText = text;
        setOpaque(false);
        setFont(new Font("微软雅黑", Font.BOLD, 18)); // 加粗并放大字体
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15)); // 增加上下边距
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.CENTER); // 文字居中
        
        // 鼠标事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                startHoverAnimation();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                startHoverAnimation();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startClickAnimation();
                if (onRefreshCallback != null) {
                    onRefreshCallback.run(); // 触发刷新回调
                }
            }
        });
        

    }
    
    public void setOnRefreshCallback(Runnable callback) {
        this.onRefreshCallback = callback;
    }
    
    public boolean isAnimating() {
        return isAnimating;
    }
    
    public String getNewText() {
        return newText;
    }
    
    @Override
    public String getText() {
        // 重写getText方法，返回当前实际显示的问题文本
        if (isAnimating) {
            // 如果正在动画中，返回当前问题（即将被替换的）
            return currentText != null ? currentText : super.getText();
        } else {
            // 如果没有在动画，返回当前问题文本
            return currentText != null ? currentText : super.getText();
        }
    }
    
    // 获取当前真实显示的问题文本（不受HTML标签影响）
    public String getCurrentQuestion() {
        String text = currentText; // 始终使用 currentText，这是当前显示的文本
        if (text == null) {
            text = super.getText();
        }
        // 清除HTML标签
        if (text != null) {
            text = text.replaceAll("<[^>]*>", "").trim();
        }
        return text != null ? text : "";
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (isHovering && hoverAnimation < 1.0f) {
                hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!isHovering && hoverAnimation > 0.0f) {
                hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }
    
    private void startClickAnimation() {
        clickAnimation = 1.0f;
        
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
        
        clickTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            clickAnimation = Math.max(0, clickAnimation - 0.08f);
            repaint();
            
            if (clickAnimation <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        clickTimer.start();
    }
    

    
    public void animateToNewText(String newText) {
        if (isAnimating) return;
        
        this.newText = newText;
        this.isAnimating = true;
        this.animationProgress = 0.0f;
        
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            animationProgress += 0.04f;
            
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                currentText = newText;
                isAnimating = false;
                animationTimer.stop();
            }
            
            repaint();
        });
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // 单层玻璃效果：半透明背景
        g2d.setColor(new Color(255, 255, 255, 25));
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 边框效果
        if (isHovering) {
            int borderAlpha = 50 + (int)(hoverAnimation * 40);
            g2d.setColor(new Color(255, 255, 255, borderAlpha));
            g2d.setStroke(new BasicStroke(1.5f + hoverAnimation * 0.3f));
        } else {
            g2d.setColor(new Color(255, 255, 255, 65));
            g2d.setStroke(new BasicStroke(1.3f));
        }
        g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        
        // 绘制点击动画效果（颜色变深）
        if (clickAnimation > 0) {
            g2d.setColor(new Color(0, 0, 0, (int)(clickAnimation * 25)));
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
        }
        

        
        // 设置文字区域
        int textAreaX = 15;
        int textAreaY = 15;
        int textAreaWidth = getWidth() - 30;
        int textAreaHeight = getHeight() - 30;
        
        // 创建文字裁剪区域
        g2d.setClip(textAreaX, textAreaY, textAreaWidth, textAreaHeight);
        
        if (isAnimating) {
            // 动画效果：原问题向上移动并淡出，新问题从下方进入并淡入
            float easeProgress = easeInOutCubic(animationProgress);
            
            // 绘制原问题（向上移动并淡出）
            float oldY = textAreaY - easeProgress * textAreaHeight;
            float oldAlpha = 1.0f - easeProgress;
            if (oldAlpha > 0) {
                g2d.setColor(new Color(255, 255, 255, (int)(oldAlpha * 255)));
                drawHTMLText(g2d, currentText, textAreaX, (int)oldY, textAreaWidth);
            }
            
            // 绘制新问题（从下方进入并淡入）
            float newY = textAreaY + textAreaHeight - easeProgress * textAreaHeight;
            float newAlpha = easeProgress;
            if (newAlpha > 0) {
                g2d.setColor(new Color(255, 255, 255, (int)(newAlpha * 255)));
                drawHTMLText(g2d, newText, textAreaX, (int)newY, textAreaWidth);
            }
        } else {
            // 非动画状态，正常绘制
            g2d.setColor(new Color(255, 255, 255));
            drawHTMLText(g2d, currentText, textAreaX, textAreaY, textAreaWidth);
        }
        
        g2d.dispose();
    }
    
    private void drawHTMLText(Graphics2D g2d, String htmlText, int x, int y, int width) {
        // 简单的HTML文本绘制（处理基本的HTML标签）
        String plainText = htmlText.replaceAll("<[^>]*>", "").trim();
        
        // 使用字体度量来绘制多行文本（居中对齐）
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = plainText.split(" ");
        StringBuilder currentLine = new StringBuilder();
        ArrayList<String> lines = new ArrayList<>();
        
        // 先分割所有行
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            int lineWidth = fm.stringWidth(testLine);
            
            if (lineWidth <= width - 20) {
                currentLine.append(currentLine.length() > 0 ? " " : "").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        // 计算文本区域高度
        int textHeight = lines.size() * fm.getHeight();
        int textAreaHeight = getHeight() - 30; // 减去上下边距
        
        // 计算竖直居中的起始Y位置
        int startY = y + (textAreaHeight - textHeight) / 2 + fm.getAscent();
        
        // 绘制每一行（水平居中，竖直居中）
        int currentY = startY;
        for (String line : lines) {
            int lineX = x + (width - fm.stringWidth(line)) / 2;
            g2d.drawString(line, lineX, currentY);
            currentY += fm.getHeight();
        }
    }
    
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            return 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
        }
    }
    

}

// 动画文本区域类 - 具有与AnimatedQuestionLabel相同的动画效果
class AnimatedTextArea extends JTextArea {
    private String currentText;
    private String newText;
    private float animationProgress = 0.0f;
    private boolean isAnimating = false;
    private Timer animationTimer;
    private boolean isHovering = false;
    private float hoverAnimation = 0.0f;
    private Timer hoverTimer;
    private float clickAnimation = 0.0f;
    private Timer clickTimer;
    
    public AnimatedTextArea() {
        super("");
        this.currentText = "";
        this.newText = "";
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new Font("微软雅黑", Font.PLAIN, 14));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setEditable(false);
        setCursor(new Cursor(Cursor.TEXT_CURSOR));
        
        // 鼠标事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                startHoverAnimation();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                startHoverAnimation();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startClickAnimation();
            }
        });
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (isHovering && hoverAnimation < 1.0f) {
                hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!isHovering && hoverAnimation > 0.0f) {
                hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }
    
    private void startClickAnimation() {
        clickAnimation = 1.0f;
        
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
        
        clickTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            clickAnimation = Math.max(0, clickAnimation - 0.08f);
            repaint();
            
            if (clickAnimation <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        clickTimer.start();
    }
    
    public void animateToNewText(String newText) {
        if (isAnimating) return;
        
        this.newText = newText;
        this.isAnimating = true;
        this.animationProgress = 0.0f;
        
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            animationProgress += 0.04f;
            
            if (animationProgress >= 1.0f) {
                animationProgress = 1.0f;
                currentText = newText;
                isAnimating = false;
                animationTimer.stop();
            }
            
            repaint();
        });
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // 单层玻璃效果：半透明背景
        g2d.setColor(new Color(255, 255, 255, 25));
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 边框效果
        if (isHovering) {
            int borderAlpha = 50 + (int)(hoverAnimation * 40);
            g2d.setColor(new Color(255, 255, 255, borderAlpha));
            g2d.setStroke(new BasicStroke(1.5f + hoverAnimation * 0.3f));
        } else {
            g2d.setColor(new Color(255, 255, 255, 65));
            g2d.setStroke(new BasicStroke(1.3f));
        }
        g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        
        // 绘制点击动画效果（颜色变深）
        if (clickAnimation > 0) {
            g2d.setColor(new Color(0, 0, 0, (int)(clickAnimation * 25)));
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
        }
        
        // 设置文本区域
        int textAreaX = 15;
        int textAreaY = 15;
        int textAreaWidth = getWidth() - 30;
        int textAreaHeight = getHeight() - 30;
        
        // 创建文字裁剪区域
        g2d.setClip(textAreaX, textAreaY, textAreaWidth, textAreaHeight);
        
        if (isAnimating) {
            // 动画效果：原文本向上移动并淡出，新文本从下方进入并淡入
            float easeProgress = easeInOutCubic(animationProgress);
            
            // 绘制原文本（向上移动并淡出）
            float oldY = textAreaY - easeProgress * textAreaHeight;
            float oldAlpha = 1.0f - easeProgress;
            if (oldAlpha > 0) {
                g2d.setColor(new Color(255, 255, 255, (int)(oldAlpha * 255)));
                drawText(g2d, currentText, textAreaX, (int)oldY, textAreaWidth);
            }
            
            // 绘制新文本（从下方进入并淡入）
            float newY = textAreaY + textAreaHeight - easeProgress * textAreaHeight;
            float newAlpha = easeProgress;
            if (newAlpha > 0) {
                g2d.setColor(new Color(255, 255, 255, (int)(newAlpha * 255)));
                drawText(g2d, newText, textAreaX, (int)newY, textAreaWidth);
            }
        } else {
            // 非动画状态，正常绘制
            g2d.setColor(new Color(255, 255, 255));
            drawText(g2d, currentText, textAreaX, textAreaY, textAreaWidth);
        }
        
        g2d.dispose();
    }
    

    
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            return 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
        }
    }
    
    @Override
    public void setText(String text) {
        if (isAnimating) {
            // 如果正在动画中，直接设置当前文本
            currentText = text;
            newText = text;
            isAnimating = false;
        } else {
            // 直接设置文本（非动画方式）
            this.currentText = text;
            this.newText = text;
        }
        super.setText(text);
        repaint();
    }
    
    private void drawText(Graphics2D g2d, String text, int x, int y, int width) {
        if (text == null || text.trim().isEmpty()) return;
        
        // 使用字体度量来绘制多行文本
        FontMetrics fm = g2d.getFontMetrics();
        String[] lines = text.split("\n");
        
        // 从指定位置开始绘制
        int currentY = y + fm.getAscent();
        
        // 绘制每一行
        for (String line : lines) {
            // 处理长行自动换行
            ArrayList<String> wrappedLines = wrapLine(g2d, line, width - 20);
            for (String wrappedLine : wrappedLines) {
                g2d.drawString(wrappedLine, x, currentY);
                currentY += fm.getHeight();
            }
        }
    }
    
    private ArrayList<String> wrapLine(Graphics2D g2d, String line, int maxWidth) {
        ArrayList<String> wrappedLines = new ArrayList<>();
        FontMetrics fm = g2d.getFontMetrics();
        
        if (line == null || line.trim().isEmpty()) {
            return wrappedLines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            // 测试添加当前字符后的行宽
            String testLine = currentLine.toString() + c;
            int lineWidth = fm.stringWidth(testLine);
            
            if (lineWidth <= maxWidth) {
                currentLine.append(c);
            } else {
                // 当前行已满，换行
                if (currentLine.length() > 0) {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                currentLine.append(c);
            }
        }
        
        // 添加最后一行
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }
        
        return wrappedLines;
    }
    
    // 获取当前显示的文本
    public String getCurrentText() {
        return currentText != null ? currentText : "";
    }
}

// 历史记录窗口类
class HistoryWindow extends JDialog {
    private ArrayList<HistoryEntry> historyEntries;
    private ArrayList<String> importedQuestions;
    private JList<String> questionList;
    private JList<String> importedList;
    private AnimatedTextArea contentArea;
    private DefaultListModel<String> listModel;
    private DefaultListModel<String> importedModel;
    private ThinkingPad parentWindow;
    private DynamicBackgroundPanel backgroundPanel;
    private Timer animationTimer;
    private ArrayList<Particle> particles = new ArrayList<>();
    private JTabbedPane tabbedPane;
    private JTabbedPane mainTabbedPane;
    private float backgroundDimAlpha = 0.0f;
    private JPanel backgroundDimPanel;
    
    public HistoryWindow(JFrame parent, ThinkingPad parentWindow) {
        super(parent, "查看", true);
        this.parentWindow = parentWindow;
        initializeHistory();
        initializeImportedQuestions();
        createGUI();
        loadHistory();
        loadImportedQuestions();
        startAnimation();
    }
    
    private void initializeHistory() {
        historyEntries = new ArrayList<>();
        particles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle());
        }
    }
    
    private void initializeImportedQuestions() {
        importedQuestions = new ArrayList<>();
    }
    
    private void loadImportedQuestions() {
        try {
            String filePath = System.getProperty("user.dir") + File.separator + "imported_questions.txt";
            File file = new File(filePath);
            if (file.exists()) {
                importedQuestions.clear();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            importedQuestions.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("加载已导入问题失败: " + e.getMessage());
        }
    }


    
    private void startAnimation() {
        animationTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            AnimationConstants.recordFrameRender();
            updateParticles();
            if (backgroundPanel != null) {
                backgroundPanel.repaint();
            }
        });
        animationTimer.start();
    }
    
    private void updateParticles() {
        for (Particle particle : particles) {
            particle.update();
        }
    }
    
    private void createGUI() {
        // 设置与主窗口相同的大小和位置
        setSize(parentWindow.frame.getSize());
        setLocation(parentWindow.frame.getLocation());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // 使用动态背景面板（显示背景图片）
        backgroundPanel = new DynamicBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout(20, 20));
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 单层玻璃效果：半透明背景
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.fillRoundRect(0, 0, width, height, 25, 25);
                
                // 边框效果
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 24, 24);
                
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // 标题
        JLabel titleLabel = new JLabel("查看", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 255, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // 添加文字阴影
        titleLabel = new JLabel("查看", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // 绘制阴影
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2 + 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + 2;
                g2d.drawString(getText(), x, y);
                
                // 绘制主文字
                g2d.setColor(getForeground());
                x = (getWidth() - fm.stringWidth(getText())) / 2;
                y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
        };
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 32));
        titleLabel.setForeground(new Color(255, 255, 255));
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 创建选项卡面板
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setOpaque(false);
        
        // 创建历史记录选项卡
        JPanel historyTab = createHistoryTab();
        
        // 创建已导入问题选项卡
        JPanel importedTab = createImportedTab();
        
        // 添加选项卡
        tabbedPane.addTab("历史记录", historyTab);
        tabbedPane.addTab("已导入问题", importedTab);
        
        // 自定义选项卡样式 - 居中显示，具有和按钮一样的动画效果和玻璃效果
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            private int hoveredTab = -1;
            private float hoverAnimation = 0.0f;
            private Timer hoverTimer;
            private int tabWidth = 120; // 更小的固定选项卡宽度
            private int tabHeight = 35; // 更小的固定选项卡高度
            
            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                return tabWidth; // 固定宽度
            }
            
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return tabHeight; // 固定高度
            }
            
            @Override
            protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex,
                                     String title, Icon icon, Rectangle tabRect, Rectangle iconRect,
                                     Rectangle textRect, boolean isSelected) {
                // 居中布局文本
                super.layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect, iconRect, textRect, isSelected);
                textRect.x = tabRect.x + (tabRect.width - textRect.width) / 2;
            }
            
            protected void layoutTab(int tabPlacement, int tabIndex, int x, int y, int w, int h, int tabCount, boolean isSelected) {
                // 居中布局选项卡
                Rectangle tabRect = rects[tabIndex];
                int spacing = 8; // 更小的间距
                int totalWidth = tabCount * tabWidth + (tabCount - 1) * spacing; // 计算总宽度
                int startX = (tabbedPane.getWidth() - totalWidth) / 2; // 居中位置
                
                tabRect.x = startX + tabIndex * (tabWidth + spacing);
                tabRect.width = tabWidth;
                tabRect.height = tabHeight;
            }
            
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, 
                                            int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                float hoverAlpha = (tabIndex == hoveredTab) ? hoverAnimation : 0.0f;
                
                // 绘制圆角裁剪区域（更小的圆角）
                g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, 10, 10));
                
                // 玻璃效果背景层
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(x, y, w, h, 10, 10);
                
                // 玻璃效果高光层
                GradientPaint gradient = new GradientPaint(
                    x, y, new Color(255, 255, 255, 15),
                    x + w, y + h, new Color(255, 255, 255, 5)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(x, y, w, h, 10, 10);
                
                // 悬停效果
                if (hoverAlpha > 0) {
                    g2d.setColor(new Color(255, 255, 255, (int)(hoverAlpha * 30)));
                    g2d.fillRoundRect(x, y, w, h, 10, 10);
                }
                
                // 选中效果
                if (isSelected) {
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.fillRoundRect(x, y, w, h, 10, 10);
                }
                
                // 重置裁剪区域
                g2d.setClip(null);
                g2d.dispose();
            }
            
            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, 
                                        int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                float hoverAlpha = (tabIndex == hoveredTab) ? hoverAnimation : 0.0f;
                
                // 绘制边框（根据悬停状态动态变化）
                if (isSelected) {
                    int borderAlpha = 80 + (int)(hoverAlpha * 100);
                    g2d.setColor(new Color(255, 255, 255, Math.min(borderAlpha, 180)));
                    g2d.setStroke(new BasicStroke(1.5f + hoverAlpha * 0.5f));
                } else if (hoverAlpha > 0) {
                    int borderAlpha = 60 + (int)(hoverAlpha * 30);
                    g2d.setColor(new Color(255, 255, 255, borderAlpha));
                    g2d.setStroke(new BasicStroke(1.5f));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.setStroke(new BasicStroke(1.2f));
                }
                g2d.drawRoundRect(x, y, w-1, h-1, 10, 10);
                
                g2d.dispose();
            }
            
            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // 不绘制内容边框
            }
            
            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, 
                                             int tabIndex, Rectangle iconRect, Rectangle textRect, 
                                             boolean isSelected) {
                // 不绘制焦点指示器
            }
            
            @Override
            protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                                  Rectangle iconRect, Rectangle textRect) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                Rectangle tabRect = rects[tabIndex];
                boolean isSelected = tabIndex == tabbedPane.getSelectedIndex();
                
                // 绘制选项卡背景
                paintTabBackground(g2d, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected);
                
                // 绘制选项卡边框
                paintTabBorder(g2d, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected);
                
                // 绘制选项卡文本
                String title = tabbedPane.getTitleAt(tabIndex);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = tabRect.x + (tabRect.width - fm.stringWidth(title)) / 2;
                int textY = tabRect.y + (tabRect.height - fm.getHeight()) / 2 + fm.getAscent();
                
                // 文字颜色和字体（更小的字体）
                if (isSelected) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("微软雅黑", Font.BOLD, 13));
                } else {
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                }
                
                g2d.drawString(title, textX, textY);
                g2d.dispose();
            }
            
            // 添加鼠标监听器来处理悬停动画
            {
                tabbedPane.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int newHoveredTab = tabForCoordinate(tabbedPane, e.getX(), e.getY());
                        if (newHoveredTab != hoveredTab) {
                            hoveredTab = newHoveredTab;
                            startHoverAnimation();
                        }
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hoveredTab = -1;
                        startHoverAnimation();
                    }
                });
                
                tabbedPane.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        int clickedTab = tabForCoordinate(tabbedPane, e.getX(), e.getY());
                        if (clickedTab != -1) {
                            // 点击效果
                            startClickAnimation(clickedTab);
                        }
                    }
                });
            }
            
            private void startHoverAnimation() {
                if (hoverTimer != null && hoverTimer.isRunning()) {
                    hoverTimer.stop();
                }
                
                hoverTimer = new Timer(32, e -> {
                    boolean needsUpdate = false;
                    
                    if (hoveredTab != -1 && hoverAnimation < 1.0f) {
                        hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                        needsUpdate = true;
                    } else if (hoveredTab == -1 && hoverAnimation > 0.0f) {
                        hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                        needsUpdate = true;
                    }
                    
                    if (needsUpdate) {
                        tabbedPane.repaint();
                    } else {
                        ((Timer)e.getSource()).stop();
                    }
                });
                hoverTimer.start();
            }
            
            private void startClickAnimation(int tabIndex) {
                // 点击效果可以通过临时改变颜色来实现
                Timer clickTimer = new Timer(20, e -> {
                    tabbedPane.repaint();
                    ((Timer)e.getSource()).stop();
                });
                clickTimer.setRepeats(false);
                clickTimer.start();
            }
        });
        
        // 设置选项卡字体和颜色
        tabbedPane.setFont(new Font("微软雅黑", Font.BOLD, 14));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setBackground(new Color(255, 255, 255, 0));
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setOpaque(false);
        
        ModernButton clearButton = new ModernButton("清空历史记录", new Color(244, 67, 54), new Color(229, 57, 53));
        ModernButton deleteButton = new ModernButton("删除", new Color(255, 152, 0), new Color(245, 124, 0));
        ModernButton fullscreenButton = new ModernButton("全屏", new Color(76, 175, 80), new Color(56, 142, 60));
        ModernButton closeButton = new ModernButton("关闭", new Color(158, 158, 158), new Color(117, 117, 117));
        
        clearButton.setPreferredSize(new Dimension(150, 45));
        deleteButton.setPreferredSize(new Dimension(100, 45));
        fullscreenButton.setPreferredSize(new Dimension(100, 45));
        closeButton.setPreferredSize(new Dimension(120, 45));
        
        // 添加选项卡切换监听器
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) { // 历史记录选项卡
                clearButton.setText("清空历史记录");
                deleteButton.setToolTipText("删除选中的历史记录");
            } else if (selectedIndex == 1) { // 已导入问题选项卡
                clearButton.setText("清空导入问题");
                deleteButton.setToolTipText("删除选中的导入问题");
            }
        });
        
        clearButton.addActionListener(e -> showClearConfirmDialog());
        deleteButton.addActionListener(e -> deleteSelectedItem());
        fullscreenButton.addActionListener(e -> toggleFullscreen());
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(clearButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(fullscreenButton);
        buttonPanel.add(closeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        backgroundPanel.add(mainPanel, BorderLayout.CENTER);
        add(backgroundPanel);
        
        // 添加窗口关闭时的资源清理
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanupResources();
            }
        });
    }
    
    // 创建历史记录选项卡
    private JPanel createHistoryTab() {
        JPanel historyPanel = new JPanel(new BorderLayout(20, 20));
        historyPanel.setOpaque(false);
        historyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 中央内容面板（使用自定义黄金比例布局）
        JPanel contentPanel = new JPanel(new GoldenRatioLayout(15)); // 15像素间距
        contentPanel.setOpaque(false);
        
        // 左侧问题列表
        JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
        leftPanel.setOpaque(false);
        
        JLabel listLabel = new JLabel("问题列表", SwingConstants.CENTER);
        listLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        listLabel.setForeground(new Color(255, 255, 255));
        listLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        listModel = new DefaultListModel<>();
        questionList = new AnimatedJList(listModel, historyEntries);
        questionList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        questionList.setForeground(Color.WHITE);
        questionList.setOpaque(false);
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 添加删除按钮面板
        JPanel deleteButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        deleteButtonPanel.setOpaque(false);
        
        ModernButton listDeleteButton = new ModernButton("删除", new Color(255, 152, 0), new Color(245, 124, 0));
        listDeleteButton.setPreferredSize(new Dimension(80, 35));
        listDeleteButton.addActionListener(e -> deleteSelectedRecord());
        listDeleteButton.setToolTipText("删除选中的历史记录");
        
        deleteButtonPanel.add(listDeleteButton);
        leftPanel.add(deleteButtonPanel, BorderLayout.SOUTH);
        
        // 右键菜单
        ModernPopupMenu popupMenu = new ModernPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除记录");
        deleteItem.setForeground(Color.WHITE);
        deleteItem.addActionListener(e -> deleteSelectedRecord());
        popupMenu.add(deleteItem);
        
        questionList.setComponentPopupMenu(popupMenu);
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedContent();
            }
        });
        
        // 自定义滚动条样式
        JScrollPane leftScrollPane = new JScrollPane(questionList) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 圆角透明背景
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, width, height, 20, 20);
                
                // 边框效果
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 19, 19);
                
                super.paintComponent(g);
                g2d.dispose();
            }
        };
        leftScrollPane.setOpaque(false);
        leftScrollPane.getViewport().setOpaque(false);
        leftScrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        leftScrollPane.getVerticalScrollBar().setOpaque(false);
        leftScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        leftScrollPane.getHorizontalScrollBar().setOpaque(false);
        leftScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        leftPanel.add(listLabel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);
        
        // 右侧内容显示
        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.setOpaque(false);
        
        JLabel contentLabel = new JLabel("详细内容", SwingConstants.CENTER);
        contentLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        contentLabel.setForeground(new Color(255, 255, 255));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        contentArea = new AnimatedTextArea();
        contentArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        contentArea.setForeground(Color.WHITE);
        
        // 自定义滚动条样式
        JScrollPane rightScrollPane = new JScrollPane(contentArea) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 圆角透明背景
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, width, height, 20, 20);
                
                // 边框效果
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 19, 19);
                
                super.paintComponent(g);
                g2d.dispose();
            }
        };
        rightScrollPane.setOpaque(false);
        rightScrollPane.getViewport().setOpaque(false);
        rightScrollPane.setBorder(BorderFactory.createEmptyBorder());
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightScrollPane.getVerticalScrollBar().setOpaque(false);
        rightScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        rightScrollPane.getHorizontalScrollBar().setOpaque(false);
        rightScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        rightPanel.add(contentLabel, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);
        
        // 添加左右面板到内容面板（黄金比例布局）
        contentPanel.add(leftPanel, GoldenRatioLayout.LEFT);
        contentPanel.add(rightPanel, GoldenRatioLayout.RIGHT);
        
        historyPanel.add(contentPanel, BorderLayout.CENTER);
        
        return historyPanel;
    }
    
    // 创建已导入问题选项卡
    private JPanel createImportedTab() {
        JPanel importedPanel = new JPanel(new BorderLayout(10, 0));
        importedPanel.setOpaque(false);
        importedPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题
        JLabel titleLabel = new JLabel("已导入问题列表", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 255, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // 内容面板（与详细内容区域高度一致）
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        importedPanel.add(titleLabel, BorderLayout.NORTH);
        importedPanel.add(contentPanel, BorderLayout.CENTER);
        
        // 问题列表
        importedModel = new DefaultListModel<>();
        importedList = new AnimatedJListForImported(importedModel, importedQuestions);
        importedList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        importedList.setForeground(Color.WHITE);
        importedList.setOpaque(false);
        importedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        importedList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 添加删除按钮面板
        JPanel deleteButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        deleteButtonPanel.setOpaque(false);
        
        ModernButton listDeleteButton = new ModernButton("删除", new Color(255, 152, 0), new Color(245, 124, 0));
        listDeleteButton.setPreferredSize(new Dimension(80, 35));
        listDeleteButton.addActionListener(e -> deleteSelectedImportedQuestion());
        listDeleteButton.setToolTipText("删除选中的导入问题");
        
        deleteButtonPanel.add(listDeleteButton);
        importedPanel.add(deleteButtonPanel, BorderLayout.NORTH);
        
        // 右键菜单
        ModernPopupMenu popupMenu = new ModernPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除问题");
        deleteItem.setForeground(Color.WHITE);
        deleteItem.addActionListener(e -> deleteSelectedImportedQuestion());
        popupMenu.add(deleteItem);
        
        importedList.setComponentPopupMenu(popupMenu);
        importedList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedImportedContent();
            }
        });
        
        // 自定义滚动条样式
        JScrollPane scrollPane = new JScrollPane(importedList) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 圆角透明背景
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, width, height, 20, 20);
                
                // 边框效果
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 19, 19);
                
                super.paintComponent(g);
                g2d.dispose();
            }
        };
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        return importedPanel;
    }
    
    private void showClearConfirmDialog() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        
        // 使用"查看"窗口本身作为父窗口，而不是强制转换为JFrame
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        if (selectedIndex == 0) { // 历史记录选项卡
            ModernConfirmDialog dialog = new ModernConfirmDialog(null, 
                "确认清空", "确定要清空所有历史记录吗？此操作不可恢复！");
            
            if (dialog.showConfirmDialog()) {
                clearAllHistory();
            }
        } else if (selectedIndex == 1) { // 已导入问题选项卡
            ModernConfirmDialog dialog = new ModernConfirmDialog(null, 
                "确认清空", "确定要清空所有已导入问题吗？此操作不可恢复！");
            
            if (dialog.showConfirmDialog()) {
                clearAllImportedQuestions();
            }
        }
    }
    
    private void deleteSelectedItem() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        
        if (selectedIndex == 0) { // 历史记录选项卡
            deleteSelectedRecord();
        } else if (selectedIndex == 1) { // 已导入问题选项卡
            deleteSelectedImportedQuestion();
        }
    }
    
    private void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            
            if (gd.getFullScreenWindow() == null) {
                // 进入全屏
                frame.dispose();
                frame.setUndecorated(true);
                frame.setResizable(true);
                gd.setFullScreenWindow(frame);
                frame.setVisible(true);
            } else {
                // 退出全屏
                gd.setFullScreenWindow(null);
                frame.dispose();
                frame.setUndecorated(false);
                frame.setResizable(false);
                frame.setVisible(true);
            }
        }
    }
    
    private void clearAllHistory() {
        try {
            // 删除文件
            File file = new File("questions_data.txt");
            if (file.exists()) {
                file.delete();
            }
            
            // 清空内存中的数据
            historyEntries.clear();
            listModel.clear();
            if (contentArea instanceof AnimatedTextArea) {
                ((AnimatedTextArea)contentArea).animateToNewText("");
            } else {
                contentArea.setText("");
            }
            
            parentWindow.showNotification("清空成功", "所有历史记录已清空", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            parentWindow.showNotification("清空失败", "清空历史记录时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void clearAllImportedQuestions() {
        try {
            // 删除文件
            File file = new File("imported_questions.txt");
            if (file.exists()) {
                file.delete();
            }
            
            // 清空内存中的数据
            importedQuestions.clear();
            importedModel.clear();
            
            parentWindow.showNotification("清空成功", "所有已导入问题已清空", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            parentWindow.showNotification("清空失败", "清空已导入问题时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void deleteSelectedRecord() {
        int selectedIndex = questionList.getSelectedIndex();
        if (selectedIndex == -1) return;
        
        HistoryEntry selectedEntry = historyEntries.get(selectedIndex);
        
        ModernConfirmDialog dialog = new ModernConfirmDialog((JFrame) getOwner(), 
            "确认删除", "确定要删除这条记录吗？\n\n" + selectedEntry.getQuestion());
        
        if (dialog.showConfirmDialog()) {
            // 使用动画删除
            if (questionList instanceof AnimatedJList) {
                ((AnimatedJList)questionList).deleteWithAnimation(selectedIndex, () -> {
                    performActualHistoryDeletion(selectedIndex);
                });
            } else {
                performActualHistoryDeletion(selectedIndex);
            }
        }
    }
    
    private void deleteHistoryEntry(int index) {
        try {
            // 使用动画删除，动画完成后执行回调
            if (questionList instanceof AnimatedJList) {
                ((AnimatedJList)questionList).deleteWithAnimation(index, () -> {
                    // 动画完成后的回调
                    performActualDeletion(index);
                });
            } else {
                // 如果不是AnimatedJList，直接删除
                performActualDeletion(index);
            }
            
        } catch (Exception e) {
            parentWindow.showNotification("删除失败", "删除历史记录时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void performActualDeletion(int index) {
        try {
            // 从内存中移除
            historyEntries.remove(index);
            
            // 重新保存文件
            saveHistoryToFile();
            
            // 清空内容区域
            if (contentArea instanceof AnimatedTextArea) {
                ((AnimatedTextArea)contentArea).animateToNewText("");
            } else {
                contentArea.setText("");
            }
            
            // 同步更新主窗口的历史记录数据
            parentWindow.loadHistoryEntries(); // 重新加载主窗口的历史记录数据
            
            parentWindow.showNotification("删除成功", "历史记录已删除", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            parentWindow.showNotification("删除失败", "删除历史记录时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void performActualHistoryDeletion(int index) {
        try {
            // 从内存中移除
            historyEntries.remove(index);
            
            // 重新保存文件
            saveHistoryToFile();
            
            // 清空内容区域
            if (contentArea instanceof AnimatedTextArea) {
                ((AnimatedTextArea)contentArea).animateToNewText("");
            } else {
                contentArea.setText("");
            }
            
            // 同步更新主窗口的历史记录数据
            parentWindow.loadHistoryEntries(); // 重新加载主窗口的历史记录数据
            
            parentWindow.showNotification("删除成功", "历史记录已删除", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            parentWindow.showNotification("删除失败", "删除历史记录时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void saveHistoryToFile() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("questions_data.txt"), "UTF-8"))) {
            for (HistoryEntry entry : historyEntries) {
                writer.println("问题：" + entry.getQuestion());
                writer.println("我的思考：" + entry.getAnswer());
                writer.println("记录时间：" + entry.getTimestamp());
                writer.println("---");
            }
        } catch (Exception e) {
            System.err.println("保存历史记录失败: " + e.getMessage());
        }
    }
    

    

    
    private void loadHistory() {
        // 清空现有数据
        historyEntries.clear();
        listModel.clear();
        
        File file = new File("questions_data.txt");
        if (!file.exists()) {
            System.out.println("历史记录文件不存在: " + file.getAbsolutePath());
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            StringBuilder currentQuestion = new StringBuilder();
            StringBuilder currentAnswer = new StringBuilder();
            String currentTimestamp = "";
            boolean readingQuestion = false;
            boolean readingAnswer = false;
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("问题：")) {
                    currentQuestion = new StringBuilder(line.substring(3));
                    readingQuestion = true;
                    readingAnswer = false;
                } else if (line.startsWith("我的思考：")) {
                    currentAnswer = new StringBuilder(line.substring(5));
                    readingQuestion = false;
                    readingAnswer = true;
                } else if (line.startsWith("记录时间：")) {
                    currentTimestamp = line.substring(5);
                } else if (line.equals("---")) {
                    if (currentQuestion.length() > 0 && currentAnswer.length() > 0) {
                        HistoryEntry entry = new HistoryEntry(
                            currentQuestion.toString(),
                            currentAnswer.toString(),
                            currentTimestamp
                        );
                        historyEntries.add(entry);
                        // 添加时间戳以区分相同问题的不同回答
                        String displayText = "问题：" + entry.getQuestion();
                        if (!entry.getTimestamp().isEmpty()) {
                            displayText += " [" + entry.getTimestamp() + "]";
                        }
                        listModel.addElement(displayText);
                    }
                    currentQuestion = new StringBuilder();
                    currentAnswer = new StringBuilder();
                    currentTimestamp = "";
                    readingQuestion = false;
                    readingAnswer = false;
                } else if (readingQuestion) {
                    currentQuestion.append("\n").append(line);
                } else if (readingAnswer) {
                    currentAnswer.append("\n").append(line);
                }
            }
            
            System.out.println("历史记录加载完成，共 " + historyEntries.size() + " 条记录");
            
        } catch (Exception e) {
            parentWindow.showNotification("读取失败", "读取历史记录失败: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void showSelectedContent() {
        int selectedIndex = questionList.getSelectedIndex();
        if (selectedIndex == -1) {
            if (contentArea instanceof AnimatedTextArea) {
                ((AnimatedTextArea)contentArea).animateToNewText("");
            } else {
                contentArea.setText("");
            }
            return;
        }
        
        // 确保selectedIndex在有效范围内
        if (selectedIndex >= historyEntries.size()) {
            System.err.println("选中的索引超出历史记录范围: " + selectedIndex + " >= " + historyEntries.size());
            return;
        }
        
        HistoryEntry entry = historyEntries.get(selectedIndex);
        if (entry == null) {
            System.err.println("获取的历史记录条目为null");
            return;
        }
        
        StringBuilder content = new StringBuilder();
        content.append("问题：\n").append(entry.getQuestion()).append("\n\n");
        content.append("我的思考：\n").append(entry.getAnswer()).append("\n\n");
        content.append("记录时间：\n").append(entry.getTimestamp());
        
        System.out.println("显示历史记录内容: " + entry.getQuestion());
        
        // 使用动画效果切换文本
        if (contentArea instanceof AnimatedTextArea) {
            ((AnimatedTextArea)contentArea).animateToNewText(content.toString());
        } else {
            contentArea.setText(content.toString());
        }
        
        // 动画完成后滚动到顶部
        SwingUtilities.invokeLater(() -> {
            contentArea.setCaretPosition(0);
        });
    }
    
    // 显示选中的导入问题内容
    private void showSelectedImportedContent() {
        int selectedIndex = importedList.getSelectedIndex();
        if (selectedIndex == -1) {
            if (contentArea instanceof AnimatedTextArea) {
                ((AnimatedTextArea)contentArea).animateToNewText("");
            } else {
                contentArea.setText("");
            }
            return;
        }
        
        // 确保selectedIndex在有效范围内
        if (selectedIndex >= importedQuestions.size()) {
            System.err.println("选中的索引超出导入问题范围: " + selectedIndex + " >= " + importedQuestions.size());
            return;
        }
        
        String question = importedQuestions.get(selectedIndex);
        if (question == null || question.trim().isEmpty()) {
            System.err.println("获取的导入问题为空");
            return;
        }
        
        StringBuilder content = new StringBuilder();
        content.append("导入问题：\n").append(question).append("\n\n");
        content.append("提示：这是从文件导入的问题，点击后可以在主界面开始思考回答。");
        
        System.out.println("显示导入问题内容: " + question);
        
        // 使用动画效果切换文本
        if (contentArea instanceof AnimatedTextArea) {
            ((AnimatedTextArea)contentArea).animateToNewText(content.toString());
        } else {
            contentArea.setText(content.toString());
        }
        
        // 动画完成后滚动到顶部
        SwingUtilities.invokeLater(() -> {
            contentArea.setCaretPosition(0);
        });
    }
    
    public void cleanupResources() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        // 清理内容区域的动画资源
        if (contentArea instanceof AnimatedTextArea) {
            ((AnimatedTextArea)contentArea).cleanup();
        }
        
        // 清理列表的动画资源
        if (questionList instanceof AnimatedJList) {
            ((AnimatedJList) questionList).cleanup();
        }
        
        if (importedList instanceof AnimatedJListForImported) {
            ((AnimatedJListForImported) importedList).cleanup();
        }
    }
    
    // 删除选中的已导入问题
    private void deleteSelectedImportedQuestion() {
        int selectedIndex = importedList.getSelectedIndex();
        if (selectedIndex == -1) return;
        
        String selectedQuestion = importedModel.getElementAt(selectedIndex);
        
        ModernConfirmDialog dialog = new ModernConfirmDialog((JFrame) getOwner(), 
            "确认删除", "确定要删除这个问题吗？\n\n" + selectedQuestion);
        
        if (dialog.showConfirmDialog()) {
            // 使用动画删除
            if (importedList instanceof AnimatedJListForImported) {
                ((AnimatedJListForImported)importedList).deleteWithAnimation(selectedIndex, () -> {
                    performActualImportedDeletion(selectedIndex);
                });
            } else {
                performActualImportedDeletion(selectedIndex);
            }
        }
    }
    
    // 删除已导入问题
    private void deleteImportedQuestion(int index) {
        try {
            // 使用动画删除，动画完成后执行回调
            if (importedList instanceof AnimatedJListForImported) {
                ((AnimatedJListForImported)importedList).deleteWithAnimation(index, () -> {
                    // 动画完成后的回调
                    performActualImportedDeletion(index);
                });
            } else {
                // 如果不是AnimatedJListForImported，直接删除
                performActualImportedDeletion(index);
            }
            
        } catch (Exception e) {
            parentWindow.showNotification("删除失败", "删除问题时出错: " + e.getMessage(), 
                NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void performActualImportedDeletion(int index) {
        try {
            // 从内存中移除
            importedQuestions.remove(index);
            
            // 重新保存文件
            saveImportedQuestionsToFile();
            
            // 同步更新主窗口的导入问题数据
            parentWindow.loadImportedQuestions(); // 重新加载主窗口的导入问题数据
            
            parentWindow.showNotification("删除成功", "问题已删除", 
                NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            parentWindow.showNotification("删除失败", "删除问题时出错: " + e.getMessage(), 
                NotificationPanel.NotificationType.ERROR);
        }
    }
    
    // 保存已导入问题到文件
    private void saveImportedQuestionsToFile() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream("imported_questions.txt"), "UTF-8"))) {
            for (String question : importedQuestions) {
                writer.println(question);
            }
        } catch (Exception e) {
            System.err.println("保存已导入问题失败: " + e.getMessage());
        }
    }
    
    // 加载已导入问题
    private void loadImportedQuestionsFromFile() {
        // 清空现有数据
        importedQuestions.clear();
        importedModel.clear();
        
        File file = new File("imported_questions.txt");
        if (!file.exists()) {
            System.out.println("已导入问题文件不存在: " + file.getAbsolutePath());
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    importedQuestions.add(line);
                    importedModel.addElement(line);
                }
            }
            
            System.out.println("已导入问题加载完成，共 " + importedQuestions.size() + " 个问题");
            
        } catch (Exception e) {
            parentWindow.showNotification("读取失败", "读取已导入问题失败: " + e.getMessage(), 
                NotificationPanel.NotificationType.ERROR);
            e.printStackTrace();
        }
    }
    
    // 确认删除已导入问题
    private void confirmDeleteImportedQuestion(int selectedIndex) {
        ModernConfirmDialog dialog = new ModernConfirmDialog((JFrame) getOwner(), 
            "确认删除", "确定要删除选中的问题吗？");
        
        if (dialog.showConfirmDialog()) {
            deleteImportedQuestion(selectedIndex);
        }
    }
    
    // 历史列表单元格渲染器（使用统一的ModernListCellRenderer）
    
    // 粒子效果类（简化版）
    private class Particle {
        private float x, y;
        private float vx, vy;
        private float size;
        private float alpha;
        private Color color;
        
        public Particle() {
            x = (float) (Math.random() * 1920);
            y = (float) (Math.random() * 1080);
            vx = (float) (Math.random() * 2 - 1);
            vy = (float) (Math.random() * 2 - 1);
            size = (float) (Math.random() * 3 + 1);
            alpha = (float) (Math.random() * 0.5 + 0.3);
            
            Color[] colors = {
                new Color(59, 130, 246),
                new Color(147, 51, 234),
                new Color(236, 72, 153),
                new Color(34, 197, 94)
            };
            color = colors[(int) (Math.random() * colors.length)];
        }
        
        public void update() {
            x += vx;
            y += vy;
            
            if (x < 0 || x > 1920) vx = -vx;
            if (y < 0 || y > 1080) vy = -vy;
            
            alpha = (float) (Math.sin(System.currentTimeMillis() * 0.001 + x * 0.01) * 0.3 + 0.5);
        }
        
        public void draw(Graphics2D g2d, int panelWidth, int panelHeight) {
            float scaleX = (float) panelWidth / 1920;
            float scaleY = (float) panelHeight / 1080;
            
            float drawX = x * scaleX;
            float drawY = y * scaleY;
            float drawSize = size * Math.min(scaleX, scaleY);
            
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            g2d.fillOval((int) (drawX - drawSize/2), (int) (drawY - drawSize/2), 
                        (int) drawSize, (int) drawSize);
        }
    }
}

// 动画JList类，支持删除项的左滑淡出和下方项上移动画
class AnimatedJList extends JList<String> {
    private ArrayList<AnimatingListItem> animatingItems = new ArrayList<>();
    private Timer animationTimer;
    private DefaultListModel<String> model;
    private ArrayList<HistoryEntry> historyEntries;
    private Runnable afterAnimationCallback;
    
    public AnimatedJList(DefaultListModel<String> model, ArrayList<HistoryEntry> historyEntries) {
        super(model);
        this.model = model;
        this.historyEntries = historyEntries;
        setOpaque(false);
        setCellRenderer(new AnimatedListCellRenderer());
    }
    
    public void deleteWithAnimation(int index, Runnable callback) {
        if (index < 0 || index >= model.getSize()) return;
        
        this.afterAnimationCallback = callback;
        
        // 获取要删除的项的信息
        String itemText = model.getElementAt(index);
        
        // 创建动画项
        AnimatingListItem animatingItem = new AnimatingListItem(index, itemText);
        animatingItems.add(animatingItem);
        
        // 立即从模型中移除（但不清空历史记录，等动画完成后再清空）
        model.removeElementAt(index);
        
        // 开始动画
        startDeleteAnimation(animatingItem);
    }
    
    private void startDeleteAnimation(AnimatingListItem item) {
        item.animationProgress = 0.0f;
        item.isAnimating = true;
        
        // 如果动画定时器没有运行，启动它
        if (animationTimer == null || !animationTimer.isRunning()) {
            animationTimer = new Timer(16, e -> updateAnimation());
            animationTimer.start();
        }
    }
    
    private void updateAnimation() {
        boolean hasActiveAnimations = false;
        
        for (AnimatingListItem item : animatingItems) {
            if (item.isAnimating) {
                hasActiveAnimations = true;
                item.animationProgress += 0.04f; // 动画速度
                
                if (item.animationProgress >= 1.0f) {
                    item.animationProgress = 1.0f;
                    item.isAnimating = false;
                    
                    // 从动画列表中移除已完成的项目
                    animatingItems.remove(item);
                    
                    // 执行删除操作
                    if (afterAnimationCallback != null) {
                        SwingUtilities.invokeLater(afterAnimationCallback);
                    }
                    
                    // 如果没有更多动画，停止定时器
                    if (animatingItems.isEmpty()) {
                        animationTimer.stop();
                    }
                    
                    break; // 跳出循环，因为我们在遍历时修改了列表
                }
            }
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // 先绘制正常的项目（可能需要调整位置以支持上移动画）
        super.paintComponent(g);
        
        // 绘制动画中的项
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (AnimatingListItem item : animatingItems) {
            if (item.isAnimating) {
                paintAnimatingItem(g2d, item);
            }
        }
        
        g2d.dispose();
    }
    
    private void paintAnimatingItem(Graphics2D g2d, AnimatingListItem item) {
        // 计算动画位置和透明度
        float progress = item.animationProgress;
        float easedProgress = easeOutCubic(progress);
        
        // 左滑距离：向左滑出整个列表宽度
        int slideDistance = getWidth();
        int currentX = - (int)(slideDistance * easedProgress);
        
        // 透明度：逐渐淡出
        float alpha = 1.0f - easedProgress;
        
        // 设置透明度
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        
        // 绘制背景
        int itemHeight = getFixedCellHeight();
        if (itemHeight <= 0) {
            itemHeight = 30; // 默认高度
        }
        int y = item.originalIndex * itemHeight;
        
        // 背景
        g2d.setColor(new Color(255, 255, 255, (int)(60 * alpha)));
        g2d.fillRoundRect(currentX, y, getWidth() - 20, itemHeight - 5, 8, 8);
        
        // 文字
        g2d.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
        FontMetrics fm = g2d.getFontMetrics();
        String displayText = (item.originalIndex + 1) + ". " + item.text;
        int textX = currentX + 12;
        int textY = y + (itemHeight - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(displayText, textX, textY);
        
        // 恢复透明度
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    // 动画项类
    private class AnimatingListItem {
        int originalIndex;
        String text;
        float animationProgress;
        boolean isAnimating;
        
        AnimatingListItem(int index, String text) {
            this.originalIndex = index;
            this.text = text;
            this.animationProgress = 0.0f;
            this.isAnimating = false;
        }
    }
    
    // 自定义单元格渲染器，支持上移动画
    private class AnimatedListCellRenderer extends DefaultListCellRenderer {
        private static final Color SELECTED_BG = new Color(255, 255, 255, 60);
        private static final Color NORMAL_BG = new Color(255, 255, 255, 20);
        private static final Color SELECTED_FG = Color.WHITE;
        private static final Color NORMAL_FG = new Color(220, 220, 220);
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            label.setOpaque(true);
            label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            
            if (isSelected) {
                label.setBackground(SELECTED_BG);
                label.setForeground(SELECTED_FG);
            } else {
                label.setBackground(NORMAL_BG);
                label.setForeground(NORMAL_FG);
            }
            
            // 检查是否有正在删除的项目影响这个位置
            float yOffset = 0;
            int itemsAbove = 0; // 统计在当前项上方被删除的项目数量
            for (AnimatingListItem item : animatingItems) {
                if (item.isAnimating && item.originalIndex < index) {
                    itemsAbove++;
                    // 如果删除的项目在当前项上方，当前项需要上移
                    float progress = item.animationProgress;
                    float easedProgress = easeOutCubic(progress);
                    int itemHeight = getFixedCellHeight();
                    if (itemHeight <= 0) {
                        itemHeight = 30; // 默认高度
                    }
                    yOffset -= itemHeight * easedProgress;
                } else if (item.isAnimating && item.originalIndex == index) {
                    // 如果当前项正在被删除，不显示它
                    label.setText("");
                    label.setOpaque(false);
                    return label;
                }
            }
            
            // 如果需要上移，调整标签位置
            if (yOffset != 0) {
                label.setBounds(label.getX(), (int)(label.getY() + yOffset), label.getWidth(), label.getHeight());
            }
            
            // 添加序号（考虑已删除的项目）
            int displayIndex = index + 1 - itemsAbove;
            if (displayIndex > 0) {
                String displayText = displayIndex + ". " + value.toString();
                label.setText(displayText);
            } else {
                label.setText(value.toString());
            }
            
            return label;
        }
    }
}

// 专门用于导入问题的动画JList类
class AnimatedJListForImported extends JList<String> {
    private ArrayList<AnimatingListItem> animatingItems = new ArrayList<>();
    private Timer animationTimer;
    private DefaultListModel<String> model;
    private ArrayList<String> importedQuestions;
    private Runnable afterAnimationCallback;
    
    public AnimatedJListForImported(DefaultListModel<String> model, ArrayList<String> importedQuestions) {
        super(model);
        this.model = model;
        this.importedQuestions = importedQuestions;
        setOpaque(false);
        setCellRenderer(new ImportedQuestionListCellRenderer());
    }
    
    public void deleteWithAnimation(int index, Runnable callback) {
        if (index < 0 || index >= model.getSize()) return;
        
        this.afterAnimationCallback = callback;
        
        // 获取要删除的项的信息
        String itemText = model.getElementAt(index);
        
        // 创建动画项
        AnimatingListItem animatingItem = new AnimatingListItem(index, itemText);
        animatingItems.add(animatingItem);
        
        // 立即从模型中移除（但不清空导入问题列表，等动画完成后再清空）
        model.removeElementAt(index);
        
        // 开始动画
        startDeleteAnimation(animatingItem);
    }
    
    private void startDeleteAnimation(AnimatingListItem item) {
        item.animationProgress = 0.0f;
        item.isAnimating = true;
        
        // 如果动画定时器没有运行，启动它
        if (animationTimer == null || !animationTimer.isRunning()) {
            animationTimer = new Timer(16, e -> updateAnimation());
            animationTimer.start();
        }
    }
    
    private void updateAnimation() {
        boolean hasActiveAnimations = false;
        
        for (AnimatingListItem item : animatingItems) {
            if (item.isAnimating) {
                hasActiveAnimations = true;
                item.animationProgress += 0.04f; // 动画速度
                
                if (item.animationProgress >= 1.0f) {
                    item.animationProgress = 1.0f;
                    item.isAnimating = false;
                    
                    // 从动画列表中移除已完成的项目
                    animatingItems.remove(item);
                    
                    // 执行删除操作
                    if (afterAnimationCallback != null) {
                        SwingUtilities.invokeLater(afterAnimationCallback);
                    }
                    
                    // 如果没有更多动画，停止定时器
                    if (animatingItems.isEmpty()) {
                        animationTimer.stop();
                    }
                    
                    break; // 跳出循环，因为我们在遍历时修改了列表
                }
            }
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // 先绘制正常的项目（可能需要调整位置以支持上移动画）
        super.paintComponent(g);
        
        // 绘制动画中的项
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (AnimatingListItem item : animatingItems) {
            if (item.isAnimating) {
                paintAnimatingItem(g2d, item);
            }
        }
        
        g2d.dispose();
    }
    
    private void paintAnimatingItem(Graphics2D g2d, AnimatingListItem item) {
        // 计算动画位置和透明度
        float progress = item.animationProgress;
        float easedProgress = easeOutCubic(progress);
        
        // 左滑距离：向左滑出整个列表宽度
        int slideDistance = getWidth();
        int currentX = - (int)(slideDistance * easedProgress);
        
        // 透明度：逐渐淡出
        float alpha = 1.0f - easedProgress;
        
        // 设置透明度
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        
        // 绘制背景
        int itemHeight = getFixedCellHeight();
        if (itemHeight <= 0) {
            itemHeight = 45; // 导入问题使用更大的高度
        }
        int y = item.originalIndex * itemHeight;
        
        // 背景
        g2d.setColor(new Color(255, 255, 255, (int)(60 * alpha)));
        g2d.fillRoundRect(currentX, y, getWidth() - 20, itemHeight - 5, 8, 8);
        
        // 文字
        g2d.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
        FontMetrics fm = g2d.getFontMetrics();
        String displayText = (item.originalIndex + 1) + ". " + item.text;
        int textX = currentX + 15;
        int textY = y + (itemHeight - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(displayText, textX, textY);
        
        // 恢复透明度
        if (alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    
    private float easeOutCubic(float t) {
        return 1 - (float)Math.pow(1 - t, 3);
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    // 动画项类
    private class AnimatingListItem {
        int originalIndex;
        String text;
        float animationProgress;
        boolean isAnimating;
        
        AnimatingListItem(int index, String text) {
            this.originalIndex = index;
            this.text = text;
            this.animationProgress = 0.0f;
            this.isAnimating = false;
        }
    }
    
    // 自定义单元格渲染器，支持上移动画
    private class ImportedQuestionListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            label.setOpaque(true);
            label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            label.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            if (isSelected) {
                label.setBackground(new Color(255, 255, 255, 60));
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(new Color(255, 255, 255, 20));
                label.setForeground(new Color(220, 220, 220));
            }
            
            // 检查是否有正在删除的项目影响这个位置
            float yOffset = 0;
            int itemsAbove = 0; // 统计在当前项上方被删除的项目数量
            for (AnimatingListItem item : animatingItems) {
                if (item.isAnimating && item.originalIndex < index) {
                    itemsAbove++;
                    // 如果删除的项目在当前项上方，当前项需要上移
                    float progress = item.animationProgress;
                    float easedProgress = easeOutCubic(progress);
                    int itemHeight = getFixedCellHeight();
                    if (itemHeight <= 0) {
                        itemHeight = 45; // 默认高度
                    }
                    yOffset -= itemHeight * easedProgress;
                } else if (item.isAnimating && item.originalIndex == index) {
                    // 如果当前项正在被删除，不显示它
                    label.setText("");
                    label.setOpaque(false);
                    return label;
                }
            }
            
            // 如果需要上移，调整标签位置
            if (yOffset != 0) {
                label.setBounds(label.getX(), (int)(label.getY() + yOffset), label.getWidth(), label.getHeight());
            }
            
            // 添加序号（考虑已删除的项目）
            int displayIndex = index + 1 - itemsAbove;
            if (displayIndex > 0) {
                String displayText = displayIndex + ". " + value.toString();
                label.setText(displayText);
            } else {
                label.setText(value.toString());
            }
            
            return label;
        }
    }
}

// 通用列表单元格渲染器类
class ModernListCellRenderer extends DefaultListCellRenderer {
    private static final Color SELECTED_BG = new Color(255, 255, 255, 60);
    private static final Color NORMAL_BG = new Color(255, 255, 255, 20);
    private static final Color SELECTED_FG = Color.WHITE;
    private static final Color NORMAL_FG = new Color(220, 220, 220);
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        label.setOpaque(true);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        if (isSelected) {
            label.setBackground(SELECTED_BG);
            label.setForeground(SELECTED_FG);
        } else {
            label.setBackground(NORMAL_BG);
            label.setForeground(NORMAL_FG);
        }
        
        // 添加序号
        String displayText = (index + 1) + ". " + value.toString();
        label.setText(displayText);
        
        return label;
    }
}

// 现代化滚动条UI类
class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    private static final Color SCROLLBAR_COLOR = new Color(255, 255, 255, 40);
    private static final Color SCROLLBAR_HOVER_COLOR = new Color(255, 255, 255, 80);
    private static final Color SCROLLBAR_DRAG_COLOR = new Color(255, 255, 255, 120);
    private static final int THUMB_WIDTH = 8;
    
    @Override
    protected void configureScrollBarColors() {
        // 不使用默认颜色
    }
    
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }
    
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }
    
    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        return button;
    }
    
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制圆角滑块
        Color thumbColor = isDragging ? SCROLLBAR_DRAG_COLOR : 
                          (isThumbRollover() ? SCROLLBAR_HOVER_COLOR : SCROLLBAR_COLOR);
        g2d.setColor(thumbColor);
        
        // 根据滚动条方向调整尺寸
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = thumbBounds.x + (thumbBounds.width - THUMB_WIDTH) / 2;
            int y = thumbBounds.y;
            int width = THUMB_WIDTH;
            int height = thumbBounds.height;
            g2d.fillRoundRect(x, y, width, height, width, width);
        } else {
            int x = thumbBounds.x;
            int y = thumbBounds.y + (thumbBounds.height - THUMB_WIDTH) / 2;
            int width = thumbBounds.width;
            int height = THUMB_WIDTH;
            g2d.fillRoundRect(x, y, width, height, height, height);
        }
        
        g2d.dispose();
    }
    
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // 不绘制轨道，保持透明
    }
    
    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(THUMB_WIDTH + 4, super.getPreferredSize(c).height);
        } else {
            return new Dimension(super.getPreferredSize(c).width, THUMB_WIDTH + 4);
        }
    }
}

// 自定义确认对话框类（简化版本）
class ModernConfirmDialog extends JDialog {
    private boolean confirmed = false;
    private String title;
    private String message;
    
    public ModernConfirmDialog(JFrame parent, String title, String message) {
        super(parent, title, true);
        this.title = title;
        this.message = message;
        
        // 使用无边框窗口
        setUndecorated(true);
        setSize(450, 200);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        // 完全透明化设置，消除黑边
        setBackground(new Color(0, 0, 0, 0));
        getRootPane().setOpaque(false);
        getContentPane().setBackground(new Color(0, 0, 0, 0));
        
        createGUI();
        startAnimation();
    }
    
    private void createGUI() {
        // 主面板 - 使用与程序一致的玻璃效果
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // 完全透明化，确保四周无黑色区域
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, width, height);
                g2d.setComposite(AlphaComposite.SrcOver);
                
                // 仅在有内容的部分绘制黑色背景，避免四周出现黑色
                int contentWidth = width - 40;  // 左右各留20像素透明边距
                int contentHeight = height - 40; // 上下各留20像素透明边距
                
                if (contentWidth > 0 && contentHeight > 0) {
                    // 黑色背景仅覆盖内容区域
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRoundRect(20, 20, contentWidth, contentHeight, 35, 35);
                    
                    // 玻璃效果背景
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.fillRoundRect(20, 20, contentWidth, contentHeight, 35, 35);
                    
                    // 内发光边框效果
                    g2d.setColor(new Color(255, 255, 255, 60));
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawRoundRect(21, 21, contentWidth-2, contentHeight-2, 33, 33);
                    
                    // 外边框效果
                    g2d.setColor(new Color(255, 255, 255, 40));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawRoundRect(20, 20, contentWidth-1, contentHeight-1, 34, 34);
                }
                
                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);
        
        // 标题
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        
        // 消息内容
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        messageArea.setForeground(Color.WHITE);
        messageArea.setOpaque(false);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        ModernButton confirmButton = new ModernButton("确定", new Color(244, 67, 54), new Color(229, 57, 53));
        ModernButton cancelButton = new ModernButton("取消", new Color(158, 158, 158), new Color(117, 117, 117));
        
        confirmButton.setPreferredSize(new Dimension(90, 35));
        cancelButton.setPreferredSize(new Dimension(90, 35));
        
        confirmButton.addActionListener(e -> {
            confirmed = true;
            disposeWithAnimation();
        });
        
        cancelButton.addActionListener(e -> {
            confirmed = false;
            disposeWithAnimation();
        });
        
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(messageArea, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);
        
        // 添加键盘事件支持
        getRootPane().registerKeyboardAction(e -> {
            confirmed = false;
            disposeWithAnimation();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // 设置焦点以便键盘事件生效
        cancelButton.requestFocusInWindow();
    }


    
    private void startAnimation() {
        setOpacity(0.0f);
        Timer animationTimer = new Timer(10, e -> {
            float opacity = getOpacity();
            if (opacity < 0.95f) {
                setOpacity(opacity + 0.08f);
            } else {
                setOpacity(1.0f);
                ((Timer)e.getSource()).stop();
            }
        });
        animationTimer.start();
    }
    
    private void disposeWithAnimation() {
        Timer animationTimer = new Timer(10, e -> {
            float opacity = getOpacity();
            if (opacity > 0.05f) {
                setOpacity(opacity - 0.08f);
            } else {
                setOpacity(0.0f);
                ((Timer)e.getSource()).stop();
                dispose();
            }
        });
        animationTimer.start();
    }
    
    public boolean showConfirmDialog() {
        setVisible(true);
        return confirmed;
    }
}

// 历史记录条目类
class HistoryEntry {
    private String question;
    private String answer;
    private String timestamp;
    
    public HistoryEntry(String question, String answer, String timestamp) {
        this.question = question;
        this.answer = answer;
        this.timestamp = timestamp;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
}

public class ThinkingPad {
    protected JFrame frame;
    private AnimatedQuestionLabel questionLabel;
    private JTextArea answerArea;
    private ArrayList<String> questions;
    private static final String DATA_FILE = "questions_data.txt";
    private static final String QUESTIONS_FILE = "imported_questions.txt";
    private JPanel notificationContainer;
    private DynamicBackgroundPanel backgroundPanel;
    private JTabbedPane mainTabbedPane;
    
    // 系统字体设置
    private static final String DEFAULT_FONT_FAMILY = getSystemFont();
    
    // 获取系统字体，优先选择支持中文的字体
    private static String getSystemFont() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            // Windows系统优先使用微软雅黑
            return "微软雅黑";
        } else if (osName.contains("mac")) {
            // macOS系统
            return "PingFang SC";
        } else {
            // Linux或其他系统
            return "SansSerif";
        }
    }
    
    public ThinkingPad() {
        // 设置系统字体和UI属性
        setupSystemProperties();
        
        System.out.println("初始化问题...");
        initializeQuestions();
        System.out.println("创建GUI...");
        createGUI();
        System.out.println("刷新已导入问题显示...");
        // 程序启动后刷新已导入问题选项卡显示
        SwingUtilities.invokeLater(() -> {
            refreshImportedTab();
            refreshHistoryTab(); // 添加刷新历史记录选项卡
        });
        System.out.println("ThinkingPad构造完成");
    }
    
    // 设置系统属性，确保中文字体正确显示
    private void setupSystemProperties() {
        try {
            // 设置默认字体
            Font defaultFont = new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, 12);
            
            // 设置所有UI组件的默认字体
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(defaultFont));
                }
            }
            
            // 设置文件编码
            System.setProperty("file.encoding", "UTF-8");
            
            // 确保使用抗锯齿
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            System.out.println("系统字体设置完成，使用字体: " + DEFAULT_FONT_FAMILY);
            
        } catch (Exception e) {
            System.err.println("设置系统字体失败: " + e.getMessage());
        }
    }
    
    private void initializeQuestions() {
        questions = new ArrayList<>();
        loadImportedQuestionsFromFile(); // 使用不同的方法名避免冲突
    }
    
    private void loadImportedQuestionsFromFile() {
        try {
            File file = new File(QUESTIONS_FILE);
            if (file.exists()) {
                // 尝试多种编码方式读取文件
                String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
                boolean loadedSuccessfully = false;
                Exception lastException = null;
                
                for (String encoding : encodings) {
                    try {
                        questions.clear(); // 清空之前可能加载的错误数据
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(QUESTIONS_FILE), encoding))) {
                            String line;
                            int lineNumber = 0;
                            int loadedCount = 0;
                            
                            while ((line = reader.readLine()) != null) {
                                lineNumber++;
                                line = line.trim();
                                if (!line.isEmpty() && !line.startsWith("#")) { // 忽略空行和注释行
                                    // 检查是否包含乱码字符（替换字符）
                                    if (!line.contains("\uFFFD")) { // 检查Unicode替换字符
                                        questions.add(line);
                                        loadedCount++;
                                    }
                                }
                            }
                            
                            // 如果成功读取到有效问题，且没有乱码，则使用这个编码
                            if (loadedCount > 0) {
                                System.out.println("使用 " + encoding + " 编码成功加载 " + loadedCount + " 个问题（共 " + lineNumber + " 行）");
                                loadedSuccessfully = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        lastException = e;
                        continue;
                    }
                }
                
                if (!loadedSuccessfully) {
                    System.err.println("所有编码尝试都失败了，使用UTF-8加载（可能有乱码）");
                    // 最后尝试UTF-8，即使可能有乱码
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(QUESTIONS_FILE), "UTF-8"))) {
                        String line;
                        int lineNumber = 0;
                        int loadedCount = 0;
                        
                        while ((line = reader.readLine()) != null) {
                            lineNumber++;
                            line = line.trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                questions.add(line);
                                loadedCount++;
                            }
                        }
                        System.out.println("UTF-8加载完成（可能有乱码）：" + loadedCount + " 个问题（共 " + lineNumber + " 行）");
                    }
                }
                
                if (questions.isEmpty()) {
                    System.out.println("警告：问题文件存在但未找到有效问题");
                }
            } else {
                System.out.println("问题文件不存在: " + QUESTIONS_FILE);
            }
        } catch (Exception e) {
            System.err.println("加载问题文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 公开的重新加载导入问题方法，供其他类调用
    public void loadImportedQuestions() {
        // 这个方法可以被其他类调用来重新加载导入问题
        // 具体的重新加载逻辑在refreshImportedTab方法中实现
        loadImportedQuestionsFromFile(); // 先重新加载到内存
        refreshImportedTab();            // 然后刷新界面显示
    }
    
    private void importQuestions() {
        // 确保文件选择器在EDT线程中运行
        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("文本文件 (*.txt)", "txt"));
            fileChooser.setDialogTitle("选择问题文件");
            
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                // 在新线程中处理文件读取，避免阻塞UI
                new Thread(() -> {
                    try {
                        // 读取选中的文件
                        ArrayList<String> newQuestions = new ArrayList<>();
                        String line;
                        int lineNumber = 0;
                        
                        // 尝试多种编码方式读取文件
                        String[] encodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1"};
                        boolean loadedSuccessfully = false;
                        Exception lastException = null;
                        
                        for (String encoding : encodings) {
                            try {
                                newQuestions.clear(); // 清空之前可能加载的错误数据
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), encoding))) {
                                    while ((line = reader.readLine()) != null) {
                                        lineNumber++;
                                        line = line.trim();
                                        if (!line.isEmpty() && !line.startsWith("#")) {
                                            // 检查是否包含乱码字符（替换字符）
                                            if (!line.contains("\uFFFD")) { // 检查Unicode替换字符
                                                newQuestions.add(line);
                                            }
                                        }
                                    }
                                }
                                
                                // 如果成功读取到有效问题，且没有乱码，则使用这个编码
                                if (newQuestions.size() > 0) {
                                    System.out.println("使用 " + encoding + " 编码成功读取 " + newQuestions.size() + " 个问题");
                                    loadedSuccessfully = true;
                                    break;
                                }
                            } catch (Exception e) {
                                lastException = e;
                                continue;
                            }
                        }
                        
                        // 如果所有编码都失败了，最后尝试UTF-8
                        if (!loadedSuccessfully) {
                            System.err.println("所有编码尝试都失败了，使用UTF-8加载（可能有乱码）");
                            newQuestions.clear();
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), "UTF-8"))) {
                                while ((line = reader.readLine()) != null) {
                                    lineNumber++;
                                    line = line.trim();
                                    if (!line.isEmpty() && !line.startsWith("#")) {
                                        newQuestions.add(line);
                                    }
                                }
                            }
                        }
                        
                        if (newQuestions.isEmpty()) {
                            SwingUtilities.invokeLater(() -> {
                                showNotification("错误", "文件中没有找到有效的问题", NotificationPanel.NotificationType.ERROR);
                            });
                            return;
                        }
                        
                        // 保存到问题文件
                        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(QUESTIONS_FILE), "UTF-8"))) {
                            for (String question : newQuestions) {
                                writer.println(question);
                            }
                        }
                        
                        // 重新加载问题
                        questions.clear();
                        loadImportedQuestionsFromFile();
                        
                        SwingUtilities.invokeLater(() -> {
                            showNotification("导入成功", 
                                "成功导入 " + newQuestions.size() + " 个问题！文件: " + selectedFile.getName(), 
                                NotificationPanel.NotificationType.SUCCESS);
                            
                            // 刷新显示第一个问题
                            if (!questions.isEmpty() && questionLabel != null) {
                                String question = getRandomQuestion();
                                if (question != null && !question.trim().isEmpty()) {
                                    String newQuestion = "<html><div style='text-align: center; padding: 15px; line-height: 1.6;'>" + 
                                                       question + "</div></html>";
                                    questionLabel.animateToNewText(newQuestion);
                                    if (answerArea != null) {
                                        answerArea.setText("");
                                    }
                                }
                            }
                            
                            // 导入成功后刷新已导入问题选项卡
                            refreshImportedTab();
                        });
                        
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            showNotification("导入失败", e.getMessage(), NotificationPanel.NotificationType.ERROR);
                        });
                    }
                }).start();
            }
        });
    }
    
    public void showNotification(String title, String message, NotificationPanel.NotificationType type) {
        // 确保在EDT线程中执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showNotification(title, message, type));
            return;
        }
        
        if (notificationContainer == null) {
            System.err.println("通知容器未初始化");
            return;
        }
        
        // 限制同时显示的通知数量，防止内存泄漏
        int maxNotifications = 5;
        while (notificationContainer.getComponentCount() >= maxNotifications) {
            Component oldest = notificationContainer.getComponent(0);
            if (oldest instanceof NotificationPanel) {
                ((NotificationPanel) oldest).cleanup();
            }
            notificationContainer.remove(0);
        }
        
        NotificationPanel notification = new NotificationPanel(title, message, type);
        
        // 获取窗口的尺寸作为参考
        Window window = SwingUtilities.getWindowAncestor(notificationContainer);
        int windowWidth = window != null ? window.getWidth() : 900;
        int windowHeight = window != null ? window.getHeight() : 700;
        
        // 设置通知大小和位置（左侧，在按钮纵坐标位置）
        int margin = 10;
        int notificationWidth = 350;
        int notificationHeight = 80;
        
        // 计算Y位置（在按钮纵坐标位置，大约在窗口底部往上120像素处）
        int buttonAreaY = windowHeight - 120; // 按钮区域的纵坐标
        int y = buttonAreaY - (notificationContainer.getComponentCount() * (notificationHeight + 10));
        
        // 确保通知不会超出容器边界
        y = Math.max(margin, Math.min(y, windowHeight - notificationHeight - margin));
        
        // 初始X位置会在动画中设置，这里先设置一个默认值
        int x = -notificationWidth; // 从左侧外部开始
        
        notification.setBounds(x, y, notificationWidth, notificationHeight);
        notificationContainer.add(notification);
        
        // 确保容器可见并正确布局
        notificationContainer.setVisible(true);
        notificationContainer.revalidate();
        notificationContainer.repaint();
        
        // 显示通知动画
        notification.showNotification();
    }
    
    private void createGUI() {
        frame = new JFrame("ThinkingPad - 深度思考与自我探索");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        
        // 添加窗口关闭时的确认对话框
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
                    "确认退出", "确定要退出 ThinkingPad 吗？");
                
                if (dialog.showConfirmDialog()) {
                    cleanupResources();
                    System.exit(0);
                }
            }
        });
        
        // 设置整体布局
        frame.setLayout(new BorderLayout());
        
        // 使用动态背景面板
        backgroundPanel = new DynamicBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        
        // 主面板（单层玻璃效果）
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 单层玻璃效果：半透明背景
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.fillRoundRect(0, 0, width, height, 25, 25);
                
                // 边框效果
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 24, 24);
                
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // 标题面板 - 添加THINKINGPAD标题
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        // THINKINGPAD大标题
        JLabel thinkingPadLabel = new JLabel("THINKINGPAD", SwingConstants.CENTER);
        thinkingPadLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        thinkingPadLabel.setForeground(new Color(255, 255, 255));
        thinkingPadLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // 添加文字阴影
        thinkingPadLabel = new JLabel("THINKINGPAD", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // 绘制阴影
                g2d.setColor(new Color(0, 0, 0, 120));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2 + 3;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + 3;
                g2d.drawString(getText(), x, y);
                
                // 绘制主文字
                g2d.setColor(getForeground());
                x = (getWidth() - fm.stringWidth(getText())) / 2;
                y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
        };
        thinkingPadLabel.setFont(new Font("微软雅黑", Font.BOLD, 36));
        thinkingPadLabel.setForeground(new Color(255, 255, 255));
        
        // 创建选项卡面板
        mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        mainTabbedPane.setOpaque(false);
        
        // 自定义选项卡样式 - 居中显示，具有玻璃效果
        mainTabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            private int hoveredTab = -1;
            private float hoverAnimation = 0.0f;
            private Timer hoverTimer;
            private int tabWidth = 120;
            private int tabHeight = 35;
            
            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                return tabWidth;
            }
            
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return tabHeight;
            }
            
            @Override
            protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex,
                                     String title, Icon icon, Rectangle tabRect, Rectangle iconRect,
                                     Rectangle textRect, boolean isSelected) {
                super.layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect, iconRect, textRect, isSelected);
                textRect.x = tabRect.x + (tabRect.width - textRect.width) / 2;
            }
            
            protected void layoutTab(int tabPlacement, int tabIndex, int x, int y, int w, int h, int tabCount, boolean isSelected) {
                Rectangle tabRect = rects[tabIndex];
                int spacing = 8;
                int totalWidth = tabCount * tabWidth + (tabCount - 1) * spacing;
                int startX = (mainTabbedPane.getWidth() - totalWidth) / 2;
                
                tabRect.x = startX + tabIndex * (tabWidth + spacing);
                tabRect.width = tabWidth;
                tabRect.height = tabHeight;
            }
            
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, 
                                            int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                float hoverAlpha = (tabIndex == hoveredTab) ? hoverAnimation : 0.0f;
                
                g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, 10, 10));
                
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(x, y, w, h, 10, 10);
                
                GradientPaint gradient = new GradientPaint(
                    x, y, new Color(255, 255, 255, 15),
                    x + w, y + h, new Color(255, 255, 255, 5)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(x, y, w, h, 10, 10);
                
                if (hoverAlpha > 0) {
                    g2d.setColor(new Color(255, 255, 255, (int)(hoverAlpha * 30)));
                    g2d.fillRoundRect(x, y, w, h, 10, 10);
                }
                
                if (isSelected) {
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.fillRoundRect(x, y, w, h, 10, 10);
                }
                
                g2d.setClip(null);
                g2d.dispose();
            }
            
            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, 
                                        int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                float hoverAlpha = (tabIndex == hoveredTab) ? hoverAnimation : 0.0f;
                
                if (isSelected) {
                    int borderAlpha = 80 + (int)(hoverAlpha * 100);
                    g2d.setColor(new Color(255, 255, 255, Math.min(borderAlpha, 180)));
                    g2d.setStroke(new BasicStroke(1.5f + hoverAlpha * 0.5f));
                } else if (hoverAlpha > 0) {
                    int borderAlpha = 60 + (int)(hoverAlpha * 30);
                    g2d.setColor(new Color(255, 255, 255, borderAlpha));
                    g2d.setStroke(new BasicStroke(1.5f));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 80));
                    g2d.setStroke(new BasicStroke(1.2f));
                }
                g2d.drawRoundRect(x, y, w-1, h-1, 10, 10);
                
                g2d.dispose();
            }
            
            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            }
            
            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, 
                                             int tabIndex, Rectangle iconRect, Rectangle textRect, 
                                             boolean isSelected) {
            }
            
            @Override
            protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                                  Rectangle iconRect, Rectangle textRect) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                Rectangle tabRect = rects[tabIndex];
                boolean isSelected = tabIndex == mainTabbedPane.getSelectedIndex();
                
                paintTabBackground(g2d, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected);
                paintTabBorder(g2d, tabPlacement, tabIndex, tabRect.x, tabRect.y, tabRect.width, tabRect.height, isSelected);
                
                String title = mainTabbedPane.getTitleAt(tabIndex);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = tabRect.x + (tabRect.width - fm.stringWidth(title)) / 2;
                int textY = tabRect.y + (tabRect.height - fm.getHeight()) / 2 + fm.getAscent();
                
                if (isSelected) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("微软雅黑", Font.BOLD, 13));
                } else {
                    g2d.setColor(new Color(255, 255, 255));
                    g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                }
                
                g2d.drawString(title, textX, textY);
                g2d.dispose();
            }
            
            {
                mainTabbedPane.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int newHoveredTab = tabForCoordinate(mainTabbedPane, e.getX(), e.getY());
                        if (newHoveredTab != hoveredTab) {
                            hoveredTab = newHoveredTab;
                            startHoverAnimation();
                        }
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hoveredTab = -1;
                        startHoverAnimation();
                    }
                });
                
                mainTabbedPane.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        int clickedTab = tabForCoordinate(mainTabbedPane, e.getX(), e.getY());
                        if (clickedTab != -1) {
                            startClickAnimation(clickedTab);
                        }
                    }
                });
            }
            
            private void startHoverAnimation() {
                if (hoverTimer != null && hoverTimer.isRunning()) {
                    hoverTimer.stop();
                }
                
                hoverTimer = new Timer(32, e -> {
                    boolean needsUpdate = false;
                    
                    if (hoveredTab != -1 && hoverAnimation < 1.0f) {
                        hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                        needsUpdate = true;
                    } else if (hoveredTab == -1 && hoverAnimation > 0.0f) {
                        hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                        needsUpdate = true;
                    }
                    
                    if (needsUpdate) {
                        mainTabbedPane.repaint();
                    } else {
                        ((Timer)e.getSource()).stop();
                    }
                });
                hoverTimer.start();
            }
            
            private void startClickAnimation(int tabIndex) {
                Timer clickTimer = new Timer(20, e -> {
                    mainTabbedPane.repaint();
                    ((Timer)e.getSource()).stop();
                });
                clickTimer.setRepeats(false);
                clickTimer.start();
            }
        });
        
        mainTabbedPane.setFont(new Font("微软雅黑", Font.BOLD, 14));
        mainTabbedPane.setForeground(Color.WHITE);
        mainTabbedPane.setBackground(new Color(255, 255, 255, 0));
        
        titlePanel.add(thinkingPadLabel, BorderLayout.NORTH);
        titlePanel.add(mainTabbedPane, BorderLayout.CENTER);
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // 创建三个选项卡的内容
        JPanel homeTab = createHomeTab();
        JPanel historyTab = createIntegratedHistoryTab();
        JPanel importedTab = createIntegratedImportedTab();
        
        // 添加选项卡
        mainTabbedPane.addTab("首页", homeTab);
        mainTabbedPane.addTab("历史记录", historyTab);
        mainTabbedPane.addTab("已导入问题", importedTab);
        
        mainPanel.add(mainTabbedPane, BorderLayout.CENTER);
        
        // 创建动态按钮面板容器
        JPanel buttonPanelContainer = new JPanel(new BorderLayout());
        buttonPanelContainer.setOpaque(false);
        
        // 首页按钮面板 - 导入、保存、刷新、全屏、退出按钮
        JPanel homeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        homeButtonPanel.setOpaque(false);
        
        ModernButton importButton = new ModernButton("导入问题", new Color(76, 175, 80), new Color(56, 142, 60));
        ModernButton saveButton = new ModernButton("保存", new Color(33, 150, 243), new Color(21, 101, 192));
        ModernButton refreshButton = new ModernButton("刷新", new Color(156, 39, 176), new Color(123, 31, 162));
        ModernButton fullscreenButton = new ModernButton("全屏", new Color(255, 193, 7), new Color(255, 160, 0));
        ModernButton exitButton = new ModernButton("退出", new Color(244, 67, 54), new Color(229, 57, 53));
        
        // 设置按钮大小
        importButton.setPreferredSize(new Dimension(120, 45));
        saveButton.setPreferredSize(new Dimension(120, 45));
        refreshButton.setPreferredSize(new Dimension(120, 45));
        fullscreenButton.setPreferredSize(new Dimension(120, 45));
        exitButton.setPreferredSize(new Dimension(120, 45));
        
        // 按钮事件
        importButton.addActionListener(e -> importQuestions());
        saveButton.addActionListener(e -> saveAnswer());
        refreshButton.addActionListener(e -> refreshQuestion());
        fullscreenButton.addActionListener(e -> toggleMainWindowFullscreen());
        exitButton.addActionListener(e -> {
            ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
                "确认退出", "确定要退出 ThinkingPad 吗？");
            
            if (dialog.showConfirmDialog()) {
                cleanupResources();
                System.exit(0);
            }
        });
        
        homeButtonPanel.add(importButton);
        homeButtonPanel.add(saveButton);
        homeButtonPanel.add(refreshButton);
        homeButtonPanel.add(fullscreenButton);
        homeButtonPanel.add(exitButton);
        
        // 历史记录按钮面板 - 清空历史记录、删除
        JPanel historyButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        historyButtonPanel.setOpaque(false);
        
        ModernButton clearHistoryButton = new ModernButton("清空历史记录", new Color(244, 67, 54), new Color(229, 57, 53));
        ModernButton deleteHistoryButton = new ModernButton("删除", new Color(255, 152, 0), new Color(245, 124, 0));
        
        clearHistoryButton.setPreferredSize(new Dimension(150, 45));
        deleteHistoryButton.setPreferredSize(new Dimension(100, 45));
        
        clearHistoryButton.addActionListener(e -> clearAllHistoryRecords());
        deleteHistoryButton.addActionListener(e -> deleteSelectedHistoryRecord());
        
        historyButtonPanel.add(clearHistoryButton);
        historyButtonPanel.add(deleteHistoryButton);
        
        // 已导入问题按钮面板 - 清空已导入问题、删除
        JPanel importedButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        importedButtonPanel.setOpaque(false);
        
        ModernButton clearImportedButton = new ModernButton("清空已导入问题", new Color(244, 67, 54), new Color(229, 57, 53));
        ModernButton deleteImportedButton = new ModernButton("删除", new Color(255, 152, 0), new Color(245, 124, 0));
        
        clearImportedButton.setPreferredSize(new Dimension(150, 45));
        deleteImportedButton.setPreferredSize(new Dimension(100, 45));
        
        clearImportedButton.addActionListener(e -> clearAllImportedQuestions());
        deleteImportedButton.addActionListener(e -> deleteSelectedImportedQuestion());
        
        importedButtonPanel.add(clearImportedButton);
        importedButtonPanel.add(deleteImportedButton);
        
        // 添加按钮面板到容器（默认显示首页按钮）
        buttonPanelContainer.add(homeButtonPanel, BorderLayout.CENTER);
        
        // 添加选项卡切换监听器
        mainTabbedPane.addChangeListener(e -> {
            int selectedIndex = mainTabbedPane.getSelectedIndex();
            // 清除当前按钮面板
            buttonPanelContainer.removeAll();
            
            // 根据选中的选项卡显示对应的按钮面板
            switch (selectedIndex) {
                case 0: // 首页
                    buttonPanelContainer.add(homeButtonPanel, BorderLayout.CENTER);
                    break;
                case 1: // 历史记录
                    buttonPanelContainer.add(historyButtonPanel, BorderLayout.CENTER);
                    break;
                case 2: // 已导入问题
                    buttonPanelContainer.add(importedButtonPanel, BorderLayout.CENTER);
                    break;
            }
            
            buttonPanelContainer.revalidate();
            buttonPanelContainer.repaint();
        });
        
        mainPanel.add(buttonPanelContainer, BorderLayout.SOUTH);
        
        // 使用 JLayeredPane 来支持通知组件重叠
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null); // 使用null布局支持绝对定位
        
        // 初始化通知容器
        notificationContainer = new JPanel(null);
        notificationContainer.setOpaque(false);
        notificationContainer.setSize(900, 700); // 设置初始尺寸
        
        // 设置组件位置和尺寸
        mainPanel.setBounds(0, 0, 900, 700);
        notificationContainer.setBounds(0, 0, 900, 700);
        
        // 添加组件到分层窗格（使用不同的层级）
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(notificationContainer, JLayeredPane.PALETTE_LAYER); // 通知在更高层级
        
        // 添加组件大小变化监听器
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int width = layeredPane.getWidth();
                int height = layeredPane.getHeight();
                
                mainPanel.setBounds(0, 0, width, height);
                notificationContainer.setBounds(0, 0, width, height);
            }
        });
        
        backgroundPanel.add(layeredPane, BorderLayout.CENTER);
        frame.add(backgroundPanel, BorderLayout.CENTER);
        
        // 添加ESC键退出全屏的键盘监听
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    if (gd.getFullScreenWindow() == frame) {
                        // 退出全屏
                        SwingUtilities.invokeLater(() -> toggleMainWindowFullscreen());
                        return true;
                    }
                }
                return false;
            }
        });
        
        System.out.println("设置窗口可见...");
        frame.setVisible(true);
        System.out.println("窗口已设置为可见");
        
        // 确保输入框可以获得焦点
        SwingUtilities.invokeLater(() -> {
            try {
                answerArea.requestFocus();
                System.out.println("焦点设置完成");
                
                // 窗口完全显示后，如果已有导入问题，刷新已导入问题选项卡
                if (!questions.isEmpty()) {
                    refreshImportedTab();
                    System.out.println("已刷新已导入问题选项卡显示");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void cleanupResources() {
        try {
            if (frame == null) return;
            
            // 清理背景面板的动画定时器
            Container contentPane = frame.getContentPane();
            if (contentPane != null && contentPane.getComponentCount() > 0) {
                Component firstComponent = contentPane.getComponent(0);
                if (firstComponent instanceof DynamicBackgroundPanel) {
                    DynamicBackgroundPanel bgPanel = (DynamicBackgroundPanel) firstComponent;
                    bgPanel.cleanup();
                }
                // 安全地清理组件资源
                else if (firstComponent instanceof Container) {
                    Container container = (Container) firstComponent;
                    cleanupComponentsRecursive(container);
                }
            }
            
            // 清理通知容器中的所有通知
            if (notificationContainer != null) {
                for (Component comp : notificationContainer.getComponents()) {
                    if (comp instanceof NotificationPanel) {
                        ((NotificationPanel) comp).cleanup();
                    }
                }
                notificationContainer.removeAll();
            }
            
        } catch (Exception e) {
            System.err.println("清理资源时出错: " + e.getMessage());
        }
    }
    
    private void cleanupComponentsRecursive(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof ModernButton) {
                ((ModernButton) comp).cleanup();
            } else if (comp instanceof ModernTextArea) {
                ((ModernTextArea) comp).cleanup();
            } else if (comp instanceof AnimatedQuestionLabel) {
                ((AnimatedQuestionLabel) comp).cleanup();
            } else if (comp instanceof NotificationPanel) {
                ((NotificationPanel) comp).cleanup();
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof ModernTextArea) {
                    ((ModernTextArea) view).cleanup();
                }
            } else if (comp instanceof Container) {
                cleanupComponentsRecursive((Container) comp);
            }
        }
    }
    
    private String getRandomQuestion() {
        if (questions == null || questions.isEmpty()) {
            return "请先导入问题文件  文件格式要求：每行一个问题  支持.txt格式  #开头的行会被忽略为注释";
        }
        try {
            Random random = new Random();
            String question = questions.get(random.nextInt(questions.size()));
            return question != null ? question.trim() : "";
        } catch (Exception e) {
            System.err.println("获取随机问题出错: " + e.getMessage());
            return "获取问题失败，请重新导入问题文件";
        }
    }
    
    private void saveAnswer() {
        // 确保在EDT线程中执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> saveAnswer());
            return;
        }
        
        // 获取当前问题，使用新的getCurrentQuestion方法
        String question = "";
        if (questionLabel != null) {
            question = questionLabel.getCurrentQuestion();
        }
        
        // 验证问题文本，确保不包含HTML标签和多余空格
        if (question == null || question.trim().isEmpty()) {
            showNotification("提示", "无法获取当前问题，请刷新后再试", NotificationPanel.NotificationType.WARNING);
            return;
        }
        
        String answer = "";
        if (answerArea != null) {
            answer = answerArea.getText().trim();
        }
        
        if (answer.isEmpty()) {
            showNotification("提示", "请输入你的答案和思考", NotificationPanel.NotificationType.WARNING);
            return;
        }
        
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(DATA_FILE, true), "UTF-8"))) {
                writer.println("问题：" + question);
                writer.println("我的思考：" + answer);
                writer.println("记录时间：" + timestamp);
                writer.println("---");
            }
            
            showNotification("保存成功", "答案已保存到历史记录", NotificationPanel.NotificationType.SUCCESS);
            
            // 保存成功后自动刷新到下一个问题并清空输入框
            SwingUtilities.invokeLater(() -> {
                refreshQuestion();
                // 确保输入框可用并获得焦点
                if (answerArea != null) {
                    answerArea.requestFocus();
                }
            });
            
            // 保存成功后刷新历史记录选项卡
            refreshHistoryTab();
            
        } catch (Exception ex) {
            showNotification("保存失败", ex.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    private void refreshQuestion() {
        // 确保在EDT线程中执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshQuestion());
            return;
        }
        
        if (questionLabel != null && answerArea != null) {
            String question = getRandomQuestion();
            if (question != null && !question.trim().isEmpty()) {
                String newQuestion = "<html><div style='text-align: center; padding: 15px; line-height: 1.6;'>" + 
                                     question + "</div></html>";
                questionLabel.animateToNewText(newQuestion);
                answerArea.setText("");
                
                // 切换背景图片（如果有多张图片）
                if (backgroundPanel != null) {
                    backgroundPanel.switchToNextBackground();
                }
                // revalidate是不必要的，因为animateToNewText会处理重绘
            }
        }
    }
    
    // 主窗口全屏切换
    private void toggleMainWindowFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        if (gd.getFullScreenWindow() == null) {
            // 进入全屏
            frame.dispose();
            frame.setUndecorated(true);
            frame.setResizable(true);
            gd.setFullScreenWindow(frame);
            frame.setVisible(true);
            showNotification("全屏模式", "已进入全屏模式，按ESC退出", NotificationPanel.NotificationType.INFO);
            
            // 修改按钮文字为"退出全屏"
            SwingUtilities.invokeLater(() -> updateFullscreenButtonText(true));
        } else {
            // 退出全屏
            gd.setFullScreenWindow(null);
            frame.dispose();
            frame.setUndecorated(false);
            frame.setResizable(true);  // 修复：退出全屏后保持可调整大小
            frame.setVisible(true);
            showNotification("退出全屏", "已退出全屏模式", NotificationPanel.NotificationType.INFO);
            
            // 修改按钮文字为"全屏"
            SwingUtilities.invokeLater(() -> updateFullscreenButtonText(false));
        }
    }
    
    // 更新全屏按钮文字
    private void updateFullscreenButtonText(boolean isFullscreen) {
        Container contentPane = frame.getContentPane();
        findAndUpdateFullscreenButton(contentPane, isFullscreen);
    }
    
    private void findAndUpdateFullscreenButton(Container container, boolean isFullscreen) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof ModernButton) {
                ModernButton button = (ModernButton) comp;
                String currentText = button.getText();
                if (currentText.equals("全屏") || currentText.equals("退出全屏")) {
                    button.setText(isFullscreen ? "退出全屏" : "全屏");
                }
            } else if (comp instanceof Container) {
                findAndUpdateFullscreenButton((Container) comp, isFullscreen);
            }
        }
    }
    
    // 创建首页选项卡内容
    private JPanel createHomeTab() {
        JPanel homePanel = new JPanel(new BorderLayout(20, 20));
        homePanel.setOpaque(false);
        homePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 使用竖向黄金比例布局（上大下小）
        JPanel contentPanel = new JPanel(new VerticalGoldenRatioLayout(15));
        contentPanel.setOpaque(false);
        
        // 问题面板（上部分 - 61.8%）
        JPanel questionPanel = new JPanel(new BorderLayout());
        questionPanel.setOpaque(false);
        
        JLabel questionTitle = new JLabel("问题", SwingConstants.CENTER);
        questionTitle.setFont(new Font("微软雅黑", Font.BOLD, 18));
        questionTitle.setForeground(new Color(255, 255, 255));
        questionTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        questionLabel = new AnimatedQuestionLabel("<html><div style='text-align: center; padding: 15px; line-height: 1.6;'>" + 
                                           getRandomQuestion() + "</div></html>");
        
        // 设置点击刷新回调
        questionLabel.setOnRefreshCallback(() -> {
            refreshQuestion();
        });
        
        questionPanel.add(questionTitle, BorderLayout.NORTH);
        questionPanel.add(questionLabel, BorderLayout.CENTER);
        
        // 答案输入面板（下部分 - 38.2%）
        JPanel answerPanel = new JPanel(new BorderLayout());
        answerPanel.setOpaque(false);
        
        JLabel answerTitle = new JLabel("我的答案和思考", SwingConstants.CENTER);
        answerTitle.setFont(new Font("微软雅黑", Font.BOLD, 18));
        answerTitle.setForeground(new Color(255, 255, 255));
        answerTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        answerArea = new ModernTextArea();
        
        // 为文本区域添加右键菜单
        setupTextAreaContextMenu(answerArea);
        
        // 使用标准的JScrollPane，不要自定义paintComponent避免重复绘制
        JScrollPane scrollPane = new JScrollPane(answerArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        answerPanel.add(answerTitle, BorderLayout.NORTH);
        answerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 添加到内容面板
        contentPanel.add(questionPanel, VerticalGoldenRatioLayout.TOP);
        contentPanel.add(answerPanel, VerticalGoldenRatioLayout.BOTTOM);
        
        homePanel.add(contentPanel, BorderLayout.CENTER);
        
        return homePanel;
    }
    
    // 创建集成的历史记录选项卡
    private JPanel createIntegratedHistoryTab() {
        JPanel historyPanel = new JPanel(new BorderLayout(20, 20));
        historyPanel.setOpaque(false);
        historyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 中央内容面板（使用自定义黄金比例布局）
        JPanel contentPanel = new JPanel(new GoldenRatioLayout(15));
        contentPanel.setOpaque(false);
        
        // 左侧问题列表
        JPanel leftPanel = new JPanel(new BorderLayout(0, 0));
        leftPanel.setOpaque(false);
        
        JLabel listLabel = new JLabel("问题列表", SwingConstants.CENTER);
        listLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        listLabel.setForeground(new Color(255, 255, 255));
        listLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        DefaultListModel<String> historyListModel = new DefaultListModel<>();
        JList<String> historyQuestionList = new AnimatedJList(historyListModel, loadHistoryEntries());
        historyQuestionList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        historyQuestionList.setForeground(Color.WHITE);
        historyQuestionList.setOpaque(false);
        historyQuestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyQuestionList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 右键菜单
        ModernPopupMenu popupMenu = new ModernPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除记录");
        deleteItem.setForeground(Color.WHITE);
        deleteItem.addActionListener(e -> deleteHistoryRecord(historyQuestionList, historyListModel));
        popupMenu.add(deleteItem);
        
        historyQuestionList.setComponentPopupMenu(popupMenu);
        historyQuestionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showHistoryContent(historyQuestionList.getSelectedValue(), contentPanel);
            }
        });
        
        // 自定义滚动条样式（保持视觉效果但不重复绘制）
        JScrollPane leftScrollPane = new JScrollPane(historyQuestionList) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 绘制圆角背景（与按钮透明度一致）
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, width, height, 20, 20);
                
                // 绘制边框
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 19, 19);
                
                // 不调用super.paintComponent，让视口内容正常渲染
                g2d.dispose();
            }
        };
        leftScrollPane.setOpaque(false);
        leftScrollPane.getViewport().setOpaque(false);
        leftScrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        leftScrollPane.getVerticalScrollBar().setOpaque(false);
        leftScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        leftScrollPane.getHorizontalScrollBar().setOpaque(false);
        leftScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        leftPanel.add(listLabel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);
        
        // 右侧内容显示
        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.setOpaque(false);
        
        JLabel contentLabel = new JLabel("详细内容", SwingConstants.CENTER);
        contentLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        contentLabel.setForeground(new Color(255, 255, 255));
        contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        AnimatedTextArea historyContentArea = new AnimatedTextArea();
        historyContentArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        historyContentArea.setForeground(Color.WHITE);
        
        // 自定义滚动条样式
        JScrollPane rightScrollPane = new JScrollPane(historyContentArea);
        rightScrollPane.setOpaque(false);
        rightScrollPane.getViewport().setOpaque(false);
        rightScrollPane.setBorder(BorderFactory.createEmptyBorder());
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightScrollPane.getVerticalScrollBar().setOpaque(false);
        rightScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        rightScrollPane.getHorizontalScrollBar().setOpaque(false);
        rightScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        rightPanel.add(contentLabel, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);
        
        // 添加左右面板到内容面板
        contentPanel.add(leftPanel, GoldenRatioLayout.LEFT);
        contentPanel.add(rightPanel, GoldenRatioLayout.RIGHT);
        
        historyPanel.add(contentPanel, BorderLayout.CENTER);
        
        return historyPanel;
    }
    
    // 创建集成的已导入问题选项卡
    private JPanel createIntegratedImportedTab() {
        JPanel importedPanel = new JPanel(new BorderLayout(10, 0));
        importedPanel.setOpaque(false);
        importedPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("已导入问题列表", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 255, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        importedPanel.add(titleLabel, BorderLayout.NORTH);
        importedPanel.add(contentPanel, BorderLayout.CENTER);
        
        DefaultListModel<String> importedListModel = new DefaultListModel<>();
        // 初始化时就添加所有已导入问题到列表模型
        for (String question : questions) {
            importedListModel.addElement(question);
        }
        JList<String> importedList = new AnimatedJListForImported(importedListModel, questions);
        importedList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        importedList.setForeground(Color.WHITE);
        importedList.setOpaque(false);
        importedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        importedList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 右键菜单
        ModernPopupMenu importedPopupMenu = new ModernPopupMenu();
        JMenuItem deleteImportedItem = new JMenuItem("删除问题");
        deleteImportedItem.setForeground(Color.WHITE);
        deleteImportedItem.addActionListener(e -> deleteImportedQuestion(importedList, importedListModel));
        importedPopupMenu.add(deleteImportedItem);
        
        importedList.setComponentPopupMenu(importedPopupMenu);
        
        // 自定义滚动条样式（保持视觉效果但不重复绘制）
        JScrollPane importedScrollPane = new JScrollPane(importedList) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                // 绘制圆角背景（与按钮透明度一致）
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, width, height, 20, 20);
                
                // 绘制边框
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, width-2, height-2, 19, 19);
                
                // 不调用super.paintComponent，让视口内容正常渲染
                g2d.dispose();
            }
        };
        importedScrollPane.setOpaque(false);
        importedScrollPane.getViewport().setOpaque(false);
        importedScrollPane.setBorder(BorderFactory.createEmptyBorder());
        importedScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        importedScrollPane.getVerticalScrollBar().setOpaque(false);
        importedScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        importedScrollPane.getHorizontalScrollBar().setOpaque(false);
        importedScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        contentPanel.add(importedScrollPane, BorderLayout.CENTER);
        
        return importedPanel;
    }
    
    // 加载历史记录条目
    public ArrayList<HistoryEntry> loadHistoryEntries() {
        ArrayList<HistoryEntry> entries = new ArrayList<>();
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DATA_FILE), "UTF-8"))) {
                    String line;
                    String currentQuestion = "";
                    String currentAnswer = "";
                    String currentTimestamp = "";
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("问题：")) {
                            // 只有当当前问题为空时才设置新问题，避免覆盖
                            if (currentQuestion.isEmpty()) {
                                currentQuestion = line.substring(3).trim();
                            }
                        } else if (line.startsWith("我的思考：")) {
                            currentAnswer = line.substring(5).trim();
                        } else if (line.startsWith("记录时间：")) {
                            currentTimestamp = line.substring(5).trim();
                        } else if (line.equals("---")) {
                            if (!currentQuestion.isEmpty()) {
                                entries.add(new HistoryEntry(currentQuestion, currentAnswer, currentTimestamp));
                                currentQuestion = "";
                                currentAnswer = "";
                                currentTimestamp = "";
                            }
                        }
                    }
                    
                    // 处理文件末尾可能没有分隔符的情况
                    if (!currentQuestion.isEmpty()) {
                        entries.add(new HistoryEntry(currentQuestion, currentAnswer, currentTimestamp));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }
    
    // 显示历史记录内容
    private void showHistoryContent(String selectedValue, JPanel contentPanel) {
        if (selectedValue == null) return;
        
        // 提取问题文本和时间戳，确保正确去除"问题："前缀
        String questionText = selectedValue;
        String timestampText = "";
        
        // 检查是否包含时间戳信息
        if (selectedValue.startsWith("问题：")) {
            // 查找时间戳部分的开始位置
            int timestampIndex = selectedValue.lastIndexOf(" [");
            if (timestampIndex != -1 && selectedValue.endsWith("]")) {
                questionText = selectedValue.substring(3, timestampIndex).trim();
                timestampText = selectedValue.substring(timestampIndex + 2, selectedValue.length() - 1).trim();
            } else {
                questionText = selectedValue.substring(3).trim();
            }
        }
        
        // 查找对应的历史记录
        ArrayList<HistoryEntry> entries = loadHistoryEntries();
        
        // 首先尝试使用问题+时间戳精确匹配
        if (!timestampText.isEmpty()) {
            for (HistoryEntry entry : entries) {
                if (entry.getQuestion().equals(questionText) && entry.getTimestamp().equals(timestampText)) {
                    displayHistoryContent(entry, contentPanel);
                    return;
                }
            }
        }
        
        // 如果没有时间戳或时间戳匹配失败，则按原来的逻辑处理（兼容旧数据）
        for (HistoryEntry entry : entries) {
            if (entry.getQuestion().equals(questionText)) {
                displayHistoryContent(entry, contentPanel);
                break;
            }
        }
    }
    
    // 辅助方法：显示历史记录内容
    private void displayHistoryContent(HistoryEntry entry, JPanel contentPanel) {
        String content = "问题：" + entry.getQuestion() + "\n\n" +
                        "我的思考：" + entry.getAnswer() + "\n\n" +
                        "记录时间：" + entry.getTimestamp();
        
        // 正确查找右侧面板：从内容面板的黄金比例布局获取右侧组件
        GoldenRatioLayout layout = (GoldenRatioLayout) contentPanel.getLayout();
        Component rightComponent = layout.getRightComponent();
        if (rightComponent instanceof JPanel) {
            JPanel rightPanel = (JPanel) rightComponent;
            Component scrollComp = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (scrollComp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) scrollComp;
                Component viewComp = scrollPane.getViewport().getView();
                if (viewComp instanceof AnimatedTextArea) {
                    AnimatedTextArea textArea = (AnimatedTextArea) viewComp;
                    textArea.animateToNewText(content);
                } else if (viewComp instanceof JTextArea) {
                    JTextArea textArea = (JTextArea) viewComp;
                    textArea.setText(content);
                }
            }
        }
    }
    
    // 删除历史记录
    private void deleteHistoryRecord(JList<String> historyQuestionList, DefaultListModel<String> historyListModel) {
        String selectedValue = historyQuestionList.getSelectedValue();
        if (selectedValue == null) return;
        
        // 提取问题文本和时间戳，确保正确去除"问题："前缀
        String questionText = selectedValue;
        String timestampText = "";
        
        // 检查是否包含时间戳信息
        if (selectedValue.startsWith("问题：")) {
            // 查找时间戳部分的开始位置
            int timestampIndex = selectedValue.lastIndexOf(" [");
            if (timestampIndex != -1 && selectedValue.endsWith("]")) {
                questionText = selectedValue.substring(3, timestampIndex).trim();
                timestampText = selectedValue.substring(timestampIndex + 2, selectedValue.length() - 1).trim();
            } else {
                questionText = selectedValue.substring(3).trim();
            }
        }
        
        ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
            "确认删除", "确定要删除这条历史记录吗？\n\n" + questionText + 
            (timestampText.isEmpty() ? "" : "\n记录时间：" + timestampText));
        
        if (dialog.showConfirmDialog()) {
            int selectedIndex = historyQuestionList.getSelectedIndex();
            
            // 使用动画删除
            if (historyQuestionList instanceof AnimatedJList) {
                // 将变量设为final以便在lambda中使用
                final String finalQuestionText = questionText;
                final String finalSelectedValue = selectedValue;
                ((AnimatedJList)historyQuestionList).deleteWithAnimation(selectedIndex, () -> {
                    performActualHistoryDeletion(finalQuestionText, historyListModel, finalSelectedValue);
                });
            } else {
                // 如果不是AnimatedJList，直接删除
                performActualHistoryDeletion(questionText, historyListModel, selectedValue);
            }
        }
    }
    
    // 执行实际的历史记录删除操作
    private void performActualHistoryDeletion(String questionText, DefaultListModel<String> historyListModel, String selectedValue) {
        // 从selectedValue中提取时间戳
        String timestampText = "";
        if (selectedValue.startsWith("问题：")) {
            int timestampIndex = selectedValue.lastIndexOf(" [");
            if (timestampIndex != -1 && selectedValue.endsWith("]")) {
                timestampText = selectedValue.substring(timestampIndex + 2, selectedValue.length() - 1).trim();
            }
        }
        
        try {
            // 读取所有记录
            ArrayList<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DATA_FILE), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            // 逐条记录处理，支持时间戳精确匹配
            ArrayList<String> newLines = new ArrayList<>();
            boolean foundMatch = false;
            
            for (int i = 0; i < lines.size(); ) {
                // 找到一个记录的开始
                if (i < lines.size() && lines.get(i).startsWith("问题：")) {
                    String currentQuestion = lines.get(i).substring(3).trim();
                    String currentAnswer = "";
                    String currentTimestamp = "";
                    int endIndex = i + 1;
                    
                    // 查找这个记录的其他部分
                    while (endIndex < lines.size() && !lines.get(endIndex).equals("---")) {
                        String line = lines.get(endIndex);
                        if (line.startsWith("我的思考：")) {
                            currentAnswer = line.substring(5).trim();
                        } else if (line.startsWith("记录时间：")) {
                            currentTimestamp = line.substring(5).trim();
                        }
                        endIndex++;
                    }
                    
                    // 判断是否是要删除的记录
                    boolean shouldDelete = currentQuestion.equals(questionText);
                    
                    // 如果有时间戳，需要同时匹配时间戳
                    if (!timestampText.isEmpty()) {
                        shouldDelete = shouldDelete && currentTimestamp.equals(timestampText);
                    }
                    
                    if (shouldDelete && !foundMatch) {
                        // 跳过这个记录（不添加到新列表）
                        foundMatch = true;
                        i = endIndex + 1; // 跳过分隔符
                    } else {
                        // 保留这个记录
                        for (int j = i; j <= endIndex && j < lines.size(); j++) {
                            newLines.add(lines.get(j));
                        }
                        i = endIndex + 1; // 移动到下一个记录
                    }
                } else {
                    // 不是记录开始，直接添加（可能是文件开头的空行等）
                    newLines.add(lines.get(i));
                    i++;
                }
            }
            
            // 如果没有找到匹配的记录，显示错误
            if (!foundMatch) {
                showNotification("删除失败", "未找到匹配的历史记录", NotificationPanel.NotificationType.ERROR);
                return;
            }
            
            // 写回文件
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(DATA_FILE), "UTF-8"))) {
                for (String line : newLines) {
                    writer.println(line);
                }
            }
            
            // 从列表中移除选中项
            historyListModel.removeElement(selectedValue);
            showNotification("删除成功", "历史记录已删除", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            showNotification("删除失败", e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    // 删除已导入问题
    private void deleteImportedQuestion(JList<String> importedList, DefaultListModel<String> importedListModel) {
        String selectedValue = importedList.getSelectedValue();
        if (selectedValue == null) return;
        
        ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
            "确认删除", "确定要删除这个问题吗？\n\n" + selectedValue);
        
        if (dialog.showConfirmDialog()) {
            int selectedIndex = importedList.getSelectedIndex();
            
            // 使用动画删除
            if (importedList instanceof AnimatedJListForImported) {
                ((AnimatedJListForImported)importedList).deleteWithAnimation(selectedIndex, () -> {
                    performActualImportedDeletion(selectedValue, importedListModel);
                });
            } else {
                // 如果不是AnimatedJListForImported，直接删除
                performActualImportedDeletion(selectedValue, importedListModel);
            }
        }
    }
    
    // 执行实际的已导入问题删除操作
    private void performActualImportedDeletion(String selectedValue, DefaultListModel<String> importedListModel) {
        try {
            // 从内存中删除
            questions.remove(selectedValue);
            
            // 更新文件
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(QUESTIONS_FILE), "UTF-8"))) {
                for (String question : questions) {
                    writer.println(question);
                }
            }
            
            // 更新列表
            importedListModel.removeElement(selectedValue);
            showNotification("删除成功", "问题已删除", NotificationPanel.NotificationType.SUCCESS);
            
        } catch (Exception e) {
            showNotification("删除失败", e.getMessage(), NotificationPanel.NotificationType.ERROR);
        }
    }
    
    // 清空所有历史记录
    private void clearAllHistoryRecords() {
        ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
            "确认清空", "确定要清空所有历史记录吗？此操作不可恢复！");
        
        if (dialog.showConfirmDialog()) {
            try {
                // 删除文件
                File file = new File(DATA_FILE);
                if (file.exists()) {
                    file.delete();
                }
                
                showNotification("清空成功", "所有历史记录已清空", NotificationPanel.NotificationType.SUCCESS);
                
                // 刷新界面
                refreshHistoryTab();
                
            } catch (Exception e) {
                showNotification("清空失败", "清空历史记录时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
            }
        }
    }
    
    // 删除选中的历史记录
    private void deleteSelectedHistoryRecord() {
        // 获取历史记录选项卡的列表组件
        Component homeTab = mainTabbedPane.getComponentAt(0);
        Component historyTab = mainTabbedPane.getComponentAt(1);
        
        if (historyTab instanceof JPanel) {
            JPanel historyPanel = (JPanel) historyTab;
            // 查找内容面板
            for (Component comp : historyPanel.getComponents()) {
                if (comp instanceof JPanel && ((JPanel)comp).getLayout() instanceof GoldenRatioLayout) {
                    JPanel contentPanel = (JPanel) comp;
                    // 查找左侧面板
                    Component leftComponent = ((GoldenRatioLayout)contentPanel.getLayout()).getLeftComponent();
                    if (leftComponent instanceof JPanel) {
                        JPanel leftPanel = (JPanel) leftComponent;
                        Component centerComponent = ((BorderLayout)leftPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                        if (centerComponent instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) centerComponent;
                            Component viewComponent = scrollPane.getViewport().getView();
                            if (viewComponent instanceof JList) {
                                @SuppressWarnings("unchecked")
                                JList<String> historyList = (JList<String>) viewComponent;
                                String selectedValue = historyList.getSelectedValue();
                                if (selectedValue != null) {
                                    DefaultListModel<String> model = (DefaultListModel<String>) historyList.getModel();
                                    deleteHistoryRecord(historyList, model);
                                } else {
                                    showNotification("提示", "请先选择要删除的历史记录", NotificationPanel.NotificationType.WARNING);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
        showNotification("错误", "无法找到历史记录列表", NotificationPanel.NotificationType.ERROR);
    }
    
    // 清空所有已导入问题
    private void clearAllImportedQuestions() {
        ModernConfirmDialog dialog = new ModernConfirmDialog(frame, 
            "确认清空", "确定要清空所有已导入问题吗？此操作不可恢复！");
        
        if (dialog.showConfirmDialog()) {
            try {
                // 删除文件
                File file = new File(QUESTIONS_FILE);
                if (file.exists()) {
                    file.delete();
                }
                
                // 清空内存
                questions.clear();
                
                showNotification("清空成功", "所有已导入问题已清空", NotificationPanel.NotificationType.SUCCESS);
                
                // 刷新界面
                refreshImportedTab();
                
            } catch (Exception e) {
                showNotification("清空失败", "清空已导入问题时出错: " + e.getMessage(), NotificationPanel.NotificationType.ERROR);
            }
        }
    }
    
    // 删除选中的已导入问题
    private void deleteSelectedImportedQuestion() {
        // 获取已导入问题选项卡的列表组件
        Component importedTab = mainTabbedPane.getComponentAt(2);
        
        if (importedTab instanceof JPanel) {
            JPanel importedPanel = (JPanel) importedTab;
            // 查找内容面板
            Component centerComponent = ((BorderLayout)importedPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (centerComponent instanceof JPanel) {
                JPanel contentPanel = (JPanel) centerComponent;
                Component scrollComponent = ((BorderLayout)contentPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (scrollComponent instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) scrollComponent;
                    Component viewComponent = scrollPane.getViewport().getView();
                    if (viewComponent instanceof JList) {
                        @SuppressWarnings("unchecked")
                        JList<String> importedList = (JList<String>) viewComponent;
                        String selectedValue = importedList.getSelectedValue();
                        if (selectedValue != null) {
                            DefaultListModel<String> model = (DefaultListModel<String>) importedList.getModel();
                            deleteImportedQuestion(importedList, model);
                        } else {
                            showNotification("提示", "请先选择要删除的已导入问题", NotificationPanel.NotificationType.WARNING);
                        }
                        return;
                    }
                }
            }
        }
        showNotification("错误", "无法找到已导入问题列表", NotificationPanel.NotificationType.ERROR);
    }
    
    // 刷新历史记录选项卡
    private void refreshHistoryTab() {
        // 确保在EDT线程中执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshHistoryTab());
            return;
        }
        
        if (mainTabbedPane == null) return;
        
        try {
            // 获取历史记录选项卡（索引为1）
            Component historyTabComponent = mainTabbedPane.getComponentAt(1);
            if (historyTabComponent instanceof JPanel) {
                JPanel historyPanel = (JPanel) historyTabComponent;
                
                // 查找内容面板（使用GoldenRatioLayout的面板）
                for (Component comp : historyPanel.getComponents()) {
                    if (comp instanceof JPanel && ((JPanel)comp).getLayout() instanceof GoldenRatioLayout) {
                        JPanel contentPanel = (JPanel) comp;
                        GoldenRatioLayout layout = (GoldenRatioLayout) contentPanel.getLayout();
                        
                        // 获取左侧面板（问题列表）
                        Component leftComponent = layout.getLeftComponent();
                        if (leftComponent instanceof JPanel) {
                            JPanel leftPanel = (JPanel) leftComponent;
                            Component centerComponent = ((BorderLayout)leftPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                            if (centerComponent instanceof JScrollPane) {
                                JScrollPane scrollPane = (JScrollPane) centerComponent;
                                Component viewComponent = scrollPane.getViewport().getView();
                                if (viewComponent instanceof JList) {
                                    @SuppressWarnings("unchecked")
                                    JList<String> historyList = (JList<String>) viewComponent;
                                    DefaultListModel<String> model = (DefaultListModel<String>) historyList.getModel();
                                    
                                    // 清空并重新加载历史记录
                                    model.clear();
                                    ArrayList<HistoryEntry> entries = loadHistoryEntries();
                                    for (HistoryEntry entry : entries) {
                                        // 确保正确添加问题文本，添加时间戳以区分相同问题的不同回答
                                        String question = entry.getQuestion();
                                        if (question != null && !question.trim().isEmpty()) {
                                            String displayText = "问题：" + question.trim();
                                            if (!entry.getTimestamp().isEmpty()) {
                                                displayText += " [" + entry.getTimestamp() + "]";
                                            }
                                            model.addElement(displayText);
                                        }
                                    }
                                    
                                    System.out.println("历史记录已刷新，共 " + entries.size() + " 条记录");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("刷新历史记录选项卡时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 刷新已导入问题选项卡
    private void refreshImportedTab() {
        // 确保在EDT线程中执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshImportedTab());
            return;
        }
        
        if (mainTabbedPane == null) return;
        
        try {
            // 获取已导入问题选项卡（索引为2）
            Component importedTabComponent = mainTabbedPane.getComponentAt(2);
            if (importedTabComponent instanceof JPanel) {
                JPanel importedPanel = (JPanel) importedTabComponent;
                
                // 查找内容面板
                Component centerComponent = ((BorderLayout)importedPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (centerComponent instanceof JPanel) {
                    JPanel contentPanel = (JPanel) centerComponent;
                    Component scrollComponent = ((BorderLayout)contentPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                    if (scrollComponent instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) scrollComponent;
                        Component viewComponent = scrollPane.getViewport().getView();
                        if (viewComponent instanceof JList) {
                            @SuppressWarnings("unchecked")
                            JList<String> importedList = (JList<String>) viewComponent;
                            DefaultListModel<String> model = (DefaultListModel<String>) importedList.getModel();
                            
                            // 清空并重新加载已导入问题
                            model.clear();
                            for (String question : questions) {
                                model.addElement(question);
                            }
                            
                            System.out.println("已导入问题已刷新，共 " + questions.size() + " 个问题");
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("刷新已导入问题选项卡时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 设置文本区域右键菜单
    private void setupTextAreaContextMenu(JTextArea textArea) {
        ModernPopupMenu popupMenu = new ModernPopupMenu();
        
        // 复制菜单项
        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(e -> {
            if (textArea.getSelectedText() != null) {
                textArea.copy();
            }
        });
        
        // 剪切菜单项
        JMenuItem cutItem = new JMenuItem("剪切");
        cutItem.addActionListener(e -> {
            if (textArea.getSelectedText() != null) {
                textArea.cut();
            }
        });
        
        // 粘贴菜单项
        JMenuItem pasteItem = new JMenuItem("粘贴");
        pasteItem.addActionListener(e -> {
            textArea.paste();
        });
        
        // 全选菜单项
        JMenuItem selectAllItem = new JMenuItem("全选");
        selectAllItem.addActionListener(e -> {
            textArea.selectAll();
        });
        
        // 清空菜单项
        JMenuItem clearItem = new JMenuItem("清空");
        clearItem.addActionListener(e -> {
            textArea.setText("");
        });
        
        popupMenu.add(copyItem);
        popupMenu.add(cutItem);
        popupMenu.add(pasteItem);
        popupMenu.addSeparator();
        popupMenu.add(selectAllItem);
        popupMenu.add(clearItem);
        
        // 添加鼠标监听器
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    

    

    
    
    // 已导入问题列表单元格渲染器（使用统一的ModernListCellRenderer）
    
    public static void main(String[] args) {
        System.out.println("ThinkingPad启动中...");
        
        // 设置系统属性，在创建GUI之前
        try {
            // 设置文件编码
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            
            // 设置字体渲染
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            // 设置LookAndFeel为系统默认
            try {
                String systemLF = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(systemLF);
            } catch (Exception lookAndFeelException) {
                // 如果系统LookAndFeel失败，使用默认的
                System.err.println("设置系统LookAndFeel失败: " + lookAndFeelException.getMessage());
            }
            
            System.out.println("系统属性设置完成");
        } catch (Exception e) {
            System.err.println("设置系统属性失败: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            System.out.println("创建GUI...");
            try {
                new ThinkingPad();
                System.out.println("GUI创建完成");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

// 现代化右键菜单类（与按钮样式完全一致，包含完整动画效果）
class ModernPopupMenu extends JPopupMenu {
    private Timer animationTimer;
    private boolean isHovering = false;
    private float hoverAnimation = 0.0f;
    private Timer hoverTimer;
    private float fadeAlpha = 0.0f;
    private Timer fadeTimer;
    private boolean isFadingIn = false;
    private boolean isFadingOut = false;
    private Point mouseLocation;
    private Dimension targetSize;
    private Point targetLocation;
    private Point currentLocation;
    private Dimension currentSize;
    private float scaleAnimation = 0.0f;
    
    public ModernPopupMenu() {
        // 设置透明背景，让自定义绘制生效
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
        
        // 设置边框和内边距
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // 启用轻量级弹出窗口以保持透明度效果
        setLightWeightPopupEnabled(true);
        
        // 设置自定义UI来处理弹出窗口的透明度
        setUI(new ModernPopupMenuUI());
        
        // 启动动画定时器
        startAnimation();
    }


    
    private void startAnimation() {
        if (animationTimer == null || !animationTimer.isRunning()) {
            animationTimer = new Timer(32, e -> {
                // 定期重绘以保持动画流畅
                if (isFadingIn || isFadingOut || fadeAlpha > 0) {
                    repaint();
                }
            });
            animationTimer.start();
        }
    }
    
    private void startFadeInAnimation() {
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
        
        isFadingIn = true;
        isFadingOut = false;
        fadeAlpha = 0.0f;
        scaleAnimation = 0.0f;
        
        fadeTimer = new Timer(16, e -> {
            scaleAnimation += 0.05f; // 统一的动画速度
            
            if (scaleAnimation >= 1.0f) {
                scaleAnimation = 1.0f;
                fadeAlpha = 1.0f;
                isFadingIn = false;
                fadeTimer.stop();
            } else {
                // 使用统一的缓动函数，确保动画对称
                float easedProgress = easeInOutCubic(scaleAnimation);
                fadeAlpha = easedProgress;
                
                // 菜单位置和尺寸保持不变，只改变透明度
                // 这样可以确保菜单从一开始就位于正确的位置和大小
            }
            
            repaint();
        });
        fadeTimer.start();
    }
    
    // 计算正确的菜单位置，确保菜单在屏幕范围内完全可见
    private Point calculateCorrectMenuLocation(Point mousePos, Dimension menuSize) {
        // 获取屏幕尺寸
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        
        int x = mousePos.x;
        int y = mousePos.y;
        
        // 检查右边界
        if (x + menuSize.width > screenBounds.x + screenBounds.width) {
            x = screenBounds.x + screenBounds.width - menuSize.width;
        }
        
        // 检查下边界
        if (y + menuSize.height > screenBounds.y + screenBounds.height) {
            y = screenBounds.y + screenBounds.height - menuSize.height;
        }
        
        // 确保不超出左边界和上边界
        x = Math.max(screenBounds.x, x);
        y = Math.max(screenBounds.y, y);
        
        return new Point(x, y);
    }
    
    private void startFadeOutAnimation(Runnable onComplete) {
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
        
        isFadingIn = false;
        isFadingOut = true;
        
        fadeTimer = new Timer(16, e -> {
            scaleAnimation -= 0.05f; // 与展开动画相同的速度
            
            if (scaleAnimation <= 0.0f) {
                scaleAnimation = 0.0f;
                fadeAlpha = 0.0f;
                isFadingOut = false;
                fadeTimer.stop();
                
                // 动画完成后执行回调
                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                // 使用相同的缓动函数，确保对称
                float easedProgress = easeInOutCubic(scaleAnimation);
                fadeAlpha = easedProgress;
                
                // 菜单位置和尺寸保持不变，只改变透明度
            }
            
            repaint();
        });
        fadeTimer.start();
    }
    
    // 统一的缓动函数：三次缓入缓出（确保展开和收缩动画对称）
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            return 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
        }
    }
    
    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int width = getWidth();
        int height = getHeight();
        
        // 应用透明度
        if (fadeAlpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        }
        
        // 首先填充完全透明的背景
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        
        // 绘制圆角裁剪区域
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, 15, 15));
        
        // 玻璃效果背景层（根据缩放状态调整透明度）
        int backgroundAlpha = isFadingIn ? (int)(25 * scaleAnimation) : 
                             isFadingOut ? (int)(25 * scaleAnimation) : 25;
        g2d.setColor(new Color(255, 255, 255, backgroundAlpha));
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 玻璃效果高光层（根据缩放状态调整透明度）
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(255, 255, 255, (int)(15 * scaleAnimation)),
            width, height, new Color(255, 255, 255, (int)(5 * scaleAnimation))
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // 重置裁剪区域
        g2d.setClip(null);
        
        // 绘制边框（根据缩放状态调整透明度）
        int borderAlpha = isFadingIn ? (int)(80 * scaleAnimation) : 
                        isFadingOut ? (int)(80 * scaleAnimation) : 80;
        g2d.setColor(new Color(255, 255, 255, borderAlpha));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, width-1, height-1, 15, 15);
        
        g2d.dispose();
    }
    
    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 应用透明度到子组件
        if (fadeAlpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        }
        
        // 如果正在缩放动画中，需要调整子组件的绘制
        if (isFadingIn || isFadingOut) {
            // 计算缩放比例
            float scaleX = currentSize != null && targetSize != null ? 
                          (float)currentSize.width / targetSize.width : 1.0f;
            float scaleY = currentSize != null && targetSize != null ? 
                          (float)currentSize.height / targetSize.height : 1.0f;
            float scale = Math.min(scaleX, scaleY);
            
            if (scale < 1.0f) {
                // 应用缩放变换
                g2d.scale(scale, scale);
                
                // 调整绘制原点以保持居中
                int offsetX = (int)((targetSize.width - currentSize.width) / 2 / scale);
                int offsetY = (int)((targetSize.height - currentSize.height) / 2 / scale);
                g2d.translate(offsetX, offsetY);
            }
        }
        
        // 确保子组件正确绘制
        super.paintChildren(g2d);
        g2d.dispose();
    }
    
    @Override
    public void paintBorder(Graphics g) {
        // 边框已在paintComponent中绘制，这里不需要重复绘制
    }
    
    @Override
    public void addSeparator() {
        // 创建自定义分隔符
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 白色分隔线（与按钮边框一致）
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.fillRect(15, getHeight()/2, getWidth()-30, 1);
                
                g2d.dispose();
            }
        };
        separator.setPreferredSize(new Dimension(0, 12));
        separator.setBackground(new Color(0, 0, 0, 0));
        super.add(separator);
    }
    
    @Override
    public JMenuItem add(JMenuItem menuItem) {
        // 创建自定义菜单项，实现与按钮相同的动画效果
        ModernMenuItem modernMenuItem = new ModernMenuItem(menuItem.getText());
        for (ActionListener listener : menuItem.getActionListeners()) {
    modernMenuItem.addActionListener(listener);
}
        
        return super.add(modernMenuItem);
    }
    
    @Override
    public JMenuItem add(Action a) {
        ModernMenuItem menuItem = new ModernMenuItem((String) a.getValue(Action.NAME));
        menuItem.addActionListener(a);
        return super.add(menuItem);
    }
    
    @Override
    public void show(Component invoker, int x, int y) {
        // 保存鼠标位置用于动画
        this.mouseLocation = new Point(x, y);
        
        // 先获取菜单的首选尺寸
        this.targetSize = getPreferredSize();
        
        // 计算正确的目标位置（考虑屏幕边界）
        this.targetLocation = calculateCorrectMenuLocation(new Point(x, y), targetSize);
        
        // 设置初始状态
        isFadingIn = false;
        isFadingOut = false;
        fadeAlpha = 0.0f;
        scaleAnimation = 0.0f;
        currentSize = new Dimension(targetSize); // 使用目标尺寸，但透明度为0
        currentLocation = new Point(targetLocation); // 直接使用目标位置
        
        // 先调用父类方法显示菜单（使用正确的位置和尺寸）
        super.show(invoker, targetLocation.x, targetLocation.y);
        
        // 立即开始动画
        SwingUtilities.invokeLater(() -> {
            startFadeInAnimation();
        });
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // 显示时直接调用父类方法，动画已在show方法中处理
            super.setVisible(true);
        } else {
            // 隐藏时先执行收缩动画，动画完成后再真正隐藏
            if (!isFadingOut && (isFadingIn || scaleAnimation > 0)) {
                startFadeOutAnimation(() -> {
                    super.setVisible(false);
                });
            } else {
                super.setVisible(false);
            }
        }
    }
}

// 自定义菜单项类，实现与按钮相同的悬停和点击效果
class ModernMenuItem extends JMenuItem {
    private boolean isHovering = false;
    private float hoverAnimation = 0.0f;
    private Timer hoverTimer;
    private float clickAnimation = 0.0f;
    private Timer clickTimer;
    
    public ModernMenuItem(String text) {
        super(text);
        
        // 确保完全透明背景
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
        setForeground(Color.WHITE);
        setFont(new Font("微软雅黑", Font.BOLD, 13));
        setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // 鼠标事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                startHoverAnimation();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                startHoverAnimation();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startClickAnimation();
            }
        });
    }
    
    public void cleanup() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
    }
    
    private void startHoverAnimation() {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        
        hoverTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            boolean needsUpdate = false;
            
            if (isHovering && hoverAnimation < 1.0f) {
                hoverAnimation = Math.min(hoverAnimation + 0.08f, 1.0f);
                needsUpdate = true;
            } else if (!isHovering && hoverAnimation > 0.0f) {
                hoverAnimation = Math.max(hoverAnimation - 0.08f, 0.0f);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }
    
    private void startClickAnimation() {
        clickAnimation = 1.0f;
        
        if (clickTimer != null && clickTimer.isRunning()) {
            clickTimer.stop();
        }
        
        clickTimer = new Timer(AnimationConstants.getOptimalFrameInterval(), e -> {
            clickAnimation = Math.max(0, clickAnimation - 0.08f);
            repaint();
            
            if (clickAnimation <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        clickTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // 首先填充完全透明的背景
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, width, height);
        
        // 绘制圆角裁剪区域
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, width, height, 8, 8));
        
        // 菜单项背景：只有悬停时才绘制
        if (isHovering) {
            // 玻璃效果背景层
            g2d.setColor(new Color(255, 255, 255, 25));
            g2d.fillRoundRect(0, 0, width, height, 8, 8);
            
            // 悬停高亮效果
            int hoverAlpha = (int)(hoverAnimation * 30);
            g2d.setColor(new Color(255, 255, 255, hoverAlpha));
            g2d.fillRoundRect(0, 0, width, height, 8, 8);
        }
        
        // 重置裁剪区域
        g2d.setClip(null);
        
        // 绘制点击动画效果（颜色变深）
        if (clickAnimation > 0) {
            g2d.setColor(new Color(0, 0, 0, (int)(clickAnimation * 40)));
            g2d.fillRoundRect(0, 0, width, height, 8, 8);
        }
        
        // 绘制边框（仅在悬停时显示）
        if (isHovering) {
            int borderAlpha = (int)(hoverAnimation * 180);
            g2d.setColor(new Color(255, 255, 255, borderAlpha));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(0, 0, width-1, height-1, 8, 8);
        }
        
        // 绘制文字
        g2d.setColor(getForeground());
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(getText(), textX, textY);
        
        g2d.dispose();
    }
    
    @Override
    public void paintBorder(Graphics g) {
        // 边框已在paintComponent中处理
    }
}

// 自定义PopupMenuUI类，处理弹出窗口的透明度问题
class ModernPopupMenuUI extends javax.swing.plaf.basic.BasicPopupMenuUI {
    @Override
    public Popup getPopup(JPopupMenu popup, int x, int y) {
        // 创建自定义的弹出窗口，确保透明度正确处理
        Popup popupObj = super.getPopup(popup, x, y);
        
        // 尝试设置弹出窗口透明属性
        trySetPopupTransparency(popupObj);
        
        return popupObj;
    }
    
    private void trySetPopupTransparency(Popup popupObj) {
        if (popupObj == null) return;
        
        try {
            // 尝试获取实际的弹出窗口
            java.lang.reflect.Field popupField = popupObj.getClass().getDeclaredField("popup");
            popupField.setAccessible(true);
            Object popupInstance = popupField.get(popupObj);
            
            if (popupInstance instanceof java.awt.Window) {
                java.awt.Window window = (java.awt.Window) popupInstance;
                // 关键：设置窗口透明背景
                window.setBackground(new Color(0, 0, 0, 0));
                
                // 如果是JWindow，确保其支持透明度
                if (window instanceof javax.swing.JWindow) {
                    javax.swing.JWindow jWindow = (javax.swing.JWindow) window;
                    jWindow.setBackground(new Color(0, 0, 0, 0));
                }
            }
        } catch (Exception e) {
            // 静默处理，不影响正常功能
        }
    }
}