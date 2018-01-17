package uk.co.boconi.emil.obd2aa;

/**
 * Created by Emil on 19/09/2017.
 */
public class ItemData {

    String text;
    int imageId;
    int gaugenumbers;

    public ItemData(String text, Integer imageId, int gaugenumbers){
        this.text=text;
        if (imageId!=null)
            this.imageId=imageId;
        this.gaugenumbers=gaugenumbers;
    }

    public String getText(){
        return text;
    }

    public int getImageId(){
        return imageId;
    }

    public int getGaugenumber() {return  gaugenumbers; }


}

