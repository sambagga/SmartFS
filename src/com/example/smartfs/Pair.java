package com.example.smartfs;


class PairedNode{
	
	String pairedID = null;  // phone number here
	int pairKey;
	boolean selfPairApproved = false;
	boolean OtherPairApproved = false;
	String pairedDirectory;
	boolean pairedFlag = false;
	public PairedNode(String phoneNo, int key){
		this.pairedID = phoneNo;
		this.pairKey = key;
	}
	
	public void setPaired(){
		if(selfPairApproved == OtherPairApproved){
			pairedFlag = true;
		}
	}	
	
}