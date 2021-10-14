package com.github.smsilva.platform.model.v1;

public class PlatformInstanceSpec {

    private String region;
    private String provider;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "PlatformInstanceSpec{" +
                "region='" + region + '\'' + "," +
                "provider='" + provider + '\'' +
                '}';
    }

}