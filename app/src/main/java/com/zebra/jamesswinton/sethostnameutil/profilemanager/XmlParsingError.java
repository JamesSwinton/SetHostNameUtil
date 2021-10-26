package com.zebra.jamesswinton.sethostnameutil.profilemanager;

public class XmlParsingError {
  String type, description;

  public XmlParsingError(String type, String description) {
    this.type = type;
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
