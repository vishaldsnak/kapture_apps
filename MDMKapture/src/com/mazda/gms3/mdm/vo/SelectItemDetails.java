package com.mazda.gms3.mdm.vo;

public class SelectItemDetails{

	private String label=null;
	private String value=null;
	private String yearValue=null;
	private String vdsType=null;
	
	
	public String getVdsType() {
		return vdsType;
	}
	public void setVdsType(String vdsType) {
		this.vdsType = vdsType;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getYearValue() {
		return yearValue;
	}
	public void setYearValue(String yearValue) {
		this.yearValue = yearValue;
	}
	
	
	public boolean equals(Object objet)
    {
        if(objet instanceof SelectItemDetails)
        {
            SelectItemDetails p = (SelectItemDetails) objet;
            return (p.getLabel().equals(this.label) && p.getValue().equals(this.value));
        }
        else return super.equals(objet);
    }

    @Override
    public String toString()
    {
        return "SelectItemDetails(label : " + getLabel() + ", value : " + getValue() + ")";
    }
	
	
}
