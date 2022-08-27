package com.aoligei.behavioral.visitor;

/**
 * Button
 *
 * @author coder
 * @date 2022-08-26 11:08:39
 * @since 1.0.0
 */
public class Button extends AbstractResource{

    /**
     * 图标名
     */
    private final String icon;
    public Button(String name, String icon) {
        super(name);
        this.icon = icon;
    }

    @Override
    protected String accept(Visitor v) {
        return v.visitButton(this);
    }

    public String getIcon() {
        return icon;
    }

}
