package com.uj.warrantytrackerapp;

public class WarrantyItem {
    String productName, purchaseDate, warrantyDuration, expiryDate;

    public WarrantyItem(String productName, String purchaseDate, String warrantyDuration, String expiryDate) {
        this.productName = productName;
        this.purchaseDate = purchaseDate;
        this.warrantyDuration = warrantyDuration;
        this.expiryDate = expiryDate;
    }

    public String getProductName() { return productName; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getWarrantyDuration() { return warrantyDuration; }
    public String getExpiryDate() { return expiryDate; }
}
