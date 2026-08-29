package com.mazda.gms3.mdm.vo;

public class RoleDetails {

	private Long roleId=null;
	private String roleName=null;
	private String roleRefKey=null;
	private Integer priority=null;
	public Long getRoleId() {
		return roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public String getRoleRefKey() {
		return roleRefKey;
	}
	public void setRoleRefKey(String roleRefKey) {
		this.roleRefKey = roleRefKey;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	
}
