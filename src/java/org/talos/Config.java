package org.talos;

import java.util.Map;

public class Config extends Context {

    private static final long serialVersionUID = 25278573523513969L;

    @SuppressWarnings("unchecked")
    public Config(Map<String, Object> raw) {
        super(raw);
        this.defaults = new Context((Map<String, Object>) raw.get("defaults"));
    }

    public void load(String file) {
    }

    public void dump(String file) {
    }

}
