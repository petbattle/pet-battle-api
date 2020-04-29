package app.petbattle;

import java.io.Serializable;
import java.util.List;

public class DataTable implements Serializable {

    private static final long serialVersionUID = -7304814269819778382L;
    public long draw;
    public long recordsTotal;
    public long recordsFiltered;
    public List<Cat> data;
    public String error;

    public DataTable() {

    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public void setRecordsTotal(long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public void setRecordsFiltered(long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public void setData(List<Cat> data) {
        this.data = data;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getDraw() {
        return this.draw;
    }

    public long getRecordsTotal() {
        return this.recordsTotal;
    }

    public long getRecordsFiltered() {
        return this.recordsFiltered;
    }

    public List<Cat> getData() {
        return this.data;
    }

    public String getError() {
        return this.error;
    }

}
