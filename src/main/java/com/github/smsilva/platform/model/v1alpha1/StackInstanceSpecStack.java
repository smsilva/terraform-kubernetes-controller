package com.github.smsilva.platform.model.v1alpha1;

public class StackInstanceSpecStack {

    private String provider;
    private String registry;
    private String image;
    private String version;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "StackInstanceSpecStack{" +
                "provider='" + provider + '\'' +
                ", registry='" + registry + '\'' +
                ", image='" + image + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

}