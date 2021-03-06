package com.puppetlabs.http.client.metrics;

public class UrlClientTimer extends ClientTimer {
    private final String url;

    public UrlClientTimer(String metricName, String url, Metrics.MetricType metricType) {
        super(metricName, metricType);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean isCategory(Metrics.MetricCategory category) {
        return category.equals(Metrics.MetricCategory.URL);
    }
}
