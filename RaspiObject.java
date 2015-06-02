package Client;

import java.io.Serializable;

public class RaspiObject implements Serializable {
String id = "raspi001";
int currentTemp;
int changeTemp;
int fan;
int type;
public String getId() {
	return id;
}

public void setId(String id) {
	this.id = id;
}

public int getCurrentTemp() {
	return currentTemp;
}

public void setCurrentTemp(int currentTemp) {
	this.currentTemp = currentTemp;
}

public int getChangeTemp() {
	return changeTemp;
}

public void setChangeTemp(int changeTemp) {
	this.changeTemp = changeTemp;
}

public int getFan() {
	return fan;
}

public void setFan(int fan) {
	this.fan = fan;
}

public int getType() {
	return type;
}

public void setType(int type) {
	this.type = type;
}



public RaspiObject(int i, int j, int k, int m) {
	// TODO Auto-generated constructor stub
	this.currentTemp = i;
	this.changeTemp =j;
	this.fan=k;
	this.type=m;
}
}
