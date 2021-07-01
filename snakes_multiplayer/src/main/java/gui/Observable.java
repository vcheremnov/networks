package gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class Observable {
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    static public class Property {
        private String name;

        protected Property(String name) {
            this.name = getClass().getName() + ":" + name;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(property.name, listener);
    }

    protected void firePropertyChanged(PropertyChangeEvent event) {
        propertyChangeSupport.firePropertyChange(event);
    }

    protected void firePropertyChanged(Property property, int oldValue, int newValue) {
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue);
    }

    protected void firePropertyChanged(Property property, boolean oldValue, boolean newValue) {
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue);
    }

    protected void firePropertyChanged(Property property, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(property.name, oldValue, newValue);
    }
}