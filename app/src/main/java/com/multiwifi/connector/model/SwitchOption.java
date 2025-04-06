package com.multiwifi.connector.model;

/**
 * Model class for network connection method switch options
 */
public class SwitchOption {
    
    private ConnectionMethod method;
    private String title;
    private String description;
    private boolean isSelected;
    
    /**
     * Default constructor
     */
    public SwitchOption() {
        isSelected = false;
    }
    
    /**
     * Constructor with parameters
     *
     * @param method Connection method
     * @param title Title of the option
     * @param description Description of the option
     */
    public SwitchOption(ConnectionMethod method, String title, String description) {
        this.method = method;
        this.title = title;
        this.description = description;
        this.isSelected = false;
    }
    
    // Getters and setters
    
    public ConnectionMethod getMethod() {
        return method;
    }
    
    public void setMethod(ConnectionMethod method) {
        this.method = method;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
