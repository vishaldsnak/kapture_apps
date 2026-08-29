package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable holder for the outcome of an Excel import that carries the
 * Action / Marker column (A/ADD, U/UPDATE, D/DELETE).
 *
 * The importing servlet reads back four row-number buckets and renders
 * success / error / info messages from them, in the same manner the screens
 * already render import results for insert / update operations:
 *
 *   saveSuccessRows   - rows created or updated successfully (A / U / blank)
 *   saveFailureRows   - rows that failed to create / update
 *   deleteSuccessRows - rows soft-deleted successfully (D)
 *   deleteFailureRows - rows that failed to soft-delete (incl. not-found by unique combination)
 *
 * Row numbers are the 1-based Excel row numbers already used by the screens
 * (header row skipped -> first data row is No. 2).
 *
 * "fatal" flags a whole-file failure (for example the transaction could not be
 * started or committed); in that case the individual buckets are not reliable.
 */
public class ImportActionResult {

	private List<Integer> saveSuccessRows = new ArrayList<Integer>();
	private List<Integer> saveFailureRows = new ArrayList<Integer>();
	private List<Integer> deleteSuccessRows = new ArrayList<Integer>();
	private List<Integer> deleteFailureRows = new ArrayList<Integer>();
	private boolean fatal = false;

	public List<Integer> getSaveSuccessRows() {
		return saveSuccessRows;
	}
	public void setSaveSuccessRows(List<Integer> saveSuccessRows) {
		this.saveSuccessRows = saveSuccessRows;
	}
	public List<Integer> getSaveFailureRows() {
		return saveFailureRows;
	}
	public void setSaveFailureRows(List<Integer> saveFailureRows) {
		this.saveFailureRows = saveFailureRows;
	}
	public List<Integer> getDeleteSuccessRows() {
		return deleteSuccessRows;
	}
	public void setDeleteSuccessRows(List<Integer> deleteSuccessRows) {
		this.deleteSuccessRows = deleteSuccessRows;
	}
	public List<Integer> getDeleteFailureRows() {
		return deleteFailureRows;
	}
	public void setDeleteFailureRows(List<Integer> deleteFailureRows) {
		this.deleteFailureRows = deleteFailureRows;
	}
	public boolean isFatal() {
		return fatal;
	}
	public void setFatal(boolean fatal) {
		this.fatal = fatal;
	}
}
