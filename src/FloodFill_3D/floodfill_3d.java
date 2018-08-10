package FloodFill_3D;


import ij.IJ;
import static ij.IJ.error;
import ij.process.*;
import ij.*;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import java.util.*;



/**
 *
 * @author JASlotman
 */
public class floodfill_3d implements PlugIn{



    public void run(String arg){

        ImagePlus imp = WindowManager.getCurrentImage();

        int[] threshold = new int[2];
        int exclude=0;
        boolean output = true;
        boolean xway = false;
        int typeChoice = 0;

        String[] type = {"Fill above upper treshold","Fill below lower threshold","Fill between upper and lower threshold","Hysteresis"};

        if(imp.getNFrames() > 1){
            error("This plugin does not work for time series (images with more than one frame)");
            return;
        }
        if(imp.getNChannels() >1){
            error("This plugin does not work for multicolor images (images with more than one channel)");
            return;
        }


        GenericDialog gd = new GenericDialog("3D Floodfill");

        gd.addNumericField("Lower Threshold",threshold[0],0);
        gd.addNumericField("Upper Threshold",threshold[1],0);
        gd.addChoice("Filltype :",type,type[0]);
        gd.addCheckbox("Output image",true);
        gd.addCheckbox("Six Neighbours",false);
        gd.addNumericField("Exclude structures below volume",exclude,0);
        gd.showDialog();

        if (gd.wasCanceled()){
            return;
        }

        threshold[0] = (int) gd.getNextNumber();
        threshold[1] = (int) gd.getNextNumber();
        typeChoice = gd.getNextChoiceIndex();
        output = gd.getNextBoolean();
        xway = gd.getNextBoolean();
        exclude = (int) gd.getNextNumber();

        floodfill(imp, threshold, output, typeChoice, exclude, xway);

    }

    public void floodfill(ImagePlus imp, int[] thr_, boolean output_, int type_, int exclude_, boolean xway_ ){
        int xdim,ydim,zdim;
        int v,vs,x,y,z,xs,ys,zs;
        int[] thr=thr_;
        int vol_cnt=0;
        int value=0;
        int thr_value = 0;
        int thr_value2 =0;
        boolean thr_test=false;
        boolean test_xway=false;
        boolean sixway=xway_;

        Calibration c = imp.getCalibration();



        Point3D[] pta;
        try {
            pta = new getPointRois(imp).getRois();
        }catch(IndexOutOfBoundsException e){
            IJ.showMessage("no point selection present");
            return;
        }

        boolean[] pta_test = new boolean[pta.length];

        //zs = imp.getSlice();

        ImageStack ims = imp.getImageStack();

        xdim = ims.getWidth();
        ydim = ims.getHeight();
        zdim = ims.getSize();




        for(int l=0;l<pta.length;l++){

            if(!pta_test[l]){

                xs = (int) pta[l].getX();
                ys = (int) pta[l].getY();
                zs = (int) pta[l].getZ();


                //IJ.showMessage(" "+xs+" "+ys+" "+zs);

                int[] xyz = new int[] {xs,ys,zs};


                Deque<int[]> stack = new ArrayDeque<>(xdim*ydim*zdim);

                boolean[][][] fill = new boolean[xdim][ydim][zdim+1];
                boolean[][][] stackd = new boolean[xdim+2][ydim+2][zdim+3];
                int[][][] hist = new int[xdim+2][ydim+2][zdim+3];

                stack.push(xyz);
                stackd[xs+1][ys+1][zs+1]=true;

                vol_cnt=0;


                while(stack.size()>0){

                    xyz = stack.pop();
                    v = getVoxel(xyz,ims);

                    if(type_==0){
                        value = v;
                        thr_value = thr[1];
                        if(value == -1){
                            thr_test=false;
                        }else{
                            thr_test = value>thr_value;
                        }
                    }

                    if(type_==1){
                        value = v;
                        thr_value = thr[0];
                        if(value == -1){
                            thr_test=false;
                        }else{
                            thr_test = value<thr_value;
                        }
                    }

                    if(type_==2){
                        value = v;
                        thr_value = thr[0];
                        thr_value2 = thr[1];
                        if(value == -1){
                            thr_test=false;
                        }else{
                            thr_test = (value>thr_value && value < thr_value2) ;
                        }
                    }

                    if(type_==3){
                        value = v;
                        thr_value = thr[0];
                        thr_value2 = thr[1];
                        if(value == -1){
                            thr_test=false;
                        }
                        if(value >thr[1]){
                            thr_test = true;
                        }else{
                            thr_test = hist[xyz[0]+1][xyz[1]+1][xyz[2]+1]>thr[1] && value > thr[0];
                        }

                    }

                    if(thr_test && !fill[xyz[0]][xyz[1]][xyz[2]] ){


                        fill[xyz[0]][xyz[1]][xyz[2]] = true;

                        vol_cnt++;
                        x = xyz[0];
                        y = xyz[1];
                        z = xyz[2];

                        for(int i=0;i<27;i++){
                            int ud = (i%3)-1;
                            int vd = ((i/3)%3)-1;
                            int wd = (i/9)-1;

                            if(sixway){
                                if(Math.abs(ud)+Math.abs(vd)+Math.abs(wd)==1){
                                    test_xway = true;
                                }else{
                                    test_xway = false;
                                }
                            }
                            if(!sixway){
                                test_xway=true;
                            }


                            if(!(ud==0 && vd ==0 && wd==0) && test_xway){

                                if(hist[x+ud+1][y+vd+1][z+wd+1] < value){
                                    hist[x+ud+1][y+vd+1][z+wd+1] = value;
                                }


                                if(!stackd[x+ud+1][y+vd+1][z+wd+1]){
                                    int[] xyz_t = new int[] {x+ud,y+vd,z+wd};
                                    stackd[x+ud+1][y+vd+1][z+wd+1]=true;
                                    stack.push(xyz_t);
                                }

                            }

                        }
                    }
                    IJ.showStatus("Point "+(l+1)+" / "+pta.length+" stacksize: "+ stack.size()+" Filled voxels:  "+vol_cnt);

                }
                
                double xcenter =0;
                double ycenter =0;
                double zcenter =0;

                if(output_ && vol_cnt > exclude_){

                    ImageStack ims_result = new ImageStack(xdim,ydim);

                    for(int i=1;i<=zdim;i++){
                        ImageProcessor ip_result = new ByteProcessor(xdim,ydim);
                        for(int j=0;j<xdim;j++){
                            for(int k=0;k<ydim;k++){

                                if(fill[j][k][i]){
                                    ip_result.putPixel(j,k,255);
                                    xcenter = xcenter+j;
                                    ycenter = ycenter+k;
                                    zcenter = zcenter+i;
                                }

                            }
                        }
                        ims_result.addSlice(ip_result);
                        IJ.showProgress(i, zdim);

                    }

                    ImagePlus imp2 = new ImagePlus("Results_"+(l+1),ims_result);
                    imp2.setCalibration(c);
                    imp2.setDimensions(1,zdim,1);
                    imp2.show();

                }


                String unit = c.getUnit();
                double xscale = c.pixelWidth;
                double yscale = c.pixelHeight;
                double zscale = c.pixelDepth;

                double volume = xscale*yscale*zscale*vol_cnt;
                xcenter = xcenter/(double)vol_cnt;
                ycenter = ycenter/(double)vol_cnt;
                zcenter = zcenter/(double)vol_cnt;



                IJ.log(""+(l+1)+"\t "+vol_cnt+"\t "+String.format("%.3f",volume)+"\t "+xcenter+"\t "+ycenter+"\t "+zcenter+"\t "+xcenter*xscale+"\t "+ycenter*yscale+"\t "+zcenter*zscale);



                //IJ.log((l+1)+"\t "+vol_cnt+"\t "+String.format("%.3f",volume));

                IJ.log("The measured volume for point "+(l+1)+" is"+ vol_cnt +" Voxels / "+String.format("%.3f",volume)+" "+unit+"^3");
                //IJ.log("The measured volume for point "+(l+1)+" for the secondary Channel is "+ vol_cnt_s +" Voxels / "+String.format("%.3f",volume_s)+" "+unit+"^3");
                //IJ.log("The ratio secondary/primary is "+String.format("%.3f",volume_s/volume));

                for(int i=l+1;i<pta.length;i++){
                    xs = (int) pta[i].getX();
                    ys = (int) pta[i].getY();
                    if(fill[xs][ys][zs]){
                        pta_test[i] = true;
                    }

                }

            }



        }



    }

    public int getVoxel(int[] xyz_ , ImageStack ims_){
        int out;

        if(xyz_[2]<=ims_.size() && xyz_[2] > 0 ){

            ImageProcessor ip = ims_.getProcessor(xyz_[2]);

            if(xyz_[0] < ip.getWidth() && xyz_[0] >= 0 && xyz_[1] < ip.getHeight() && xyz_[1] >= 0){
                out = ip.get(xyz_[0],xyz_[1]);
            }else{
                out=-1;
            }
        }else{
            out=-1;
        }




        return out;

    }


}


