package com.egoclean.persistence;

class JuaneloMendieta extends Pepito{
    private long id;
    private String longText;
    private boolean buleano;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLongText() {
        return longText;
    }

    public void setLongText(String longText) {
        this.longText = longText;
    }

    public boolean isBuleano() {
        return buleano;
    }

    public void setBuleano(boolean buleano) {
        this.buleano = buleano;
    }
}
