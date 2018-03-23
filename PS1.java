package jforex;
import java.math.*;
import java.util.*;

import com.dukascopy.api.*;

public class PS1 implements IStrategy {
    private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	private IUserInterface userInterface;

    private int estado=2;
    private int estado_old;
    private String strategylabel="MM1";
    public double Lots =0.01;
    public double Slippage=3.0;
    private int tagCounter = 0;
    
    @Configurable ("Debug Mode") public boolean DebugMode=false;
    @Configurable ("Periodo temporal ")
    public static Period periodo = Period.ONE_HOUR;
    @Configurable ("Margen")
    public static int Margen = 200;
    	
	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();
	}

	public void onAccount(IAccount account) throws JFException {
	}

	public void onMessage(IMessage message) throws JFException {
	}

	public void onStop() throws JFException {
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}
	
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        
if (period == period.WEEKLY)
    {        
   if(DebugMode==true) calendar.setTimeInMillis(bidBar.getTime());  // esto solo es necesario  cuando estamos en backtesting..
   else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
    int tendencia=0;
    int entrada_alcista=0;
    int entrada_bajista=0;  
        piv_high = bidbar.getHigh();
        piv_low = bidbar.getLow();  
        piv_close = bidbar.getClose();
        piv_P= (piv_high+piv_low+piv_close)/3;
        piv_R2[i]= piv_P +piv_high-piv_low;
        piv_S2[i]= piv_P -piv_high+piv_low;
        piv_R3[i];
        piv_S3[i];
      
    // calculos de medias

    if (askBar.getClose()> ma1)
        {
        tendencia= 1; //alcista            
         } 
    else 
        {
        tendencia= 2; //bajista    
        } 
    //cruces
    if (ma3>ma2)
        {
        entrada_alcista=1;
        //print("cruce alcista");
        }
    else
        {
        entrada_bajista=1;  
        //print("cruce bajista");
        }      
    // maquina de estados       
    // estado 1 compra
    // estado 2 espera compra
    // estado 3 venta
    // estado 4 espera venta 
    // compra 1
    if(estado==1 || estado == 2)
        {
        if(tendencia==2)
                //pasa a los estados de ventas
             {
             if (estado==1)
                 {
                 if(orden(instrument,1) != null) orden(instrument,1).close(); 
                 print("cierra compra");   
                 }
             if (entrada_bajista==1)
                 {
                 //pasa a estado 3 venta y realiza una venta 
                 IOrder order = engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELL, Lots, 0, Slippage);
                 print("venta");                
                 estado=3;   
                 }    
             else
                 {
                 //pasa a estado 4
                 estado=4;    
                 }    
             }
        else if(estado==1 && entrada_alcista==0)
            {
            //cierra compra  y pasa a estado 2
            if(orden(instrument,1) != null) orden(instrument,1).close();    
            print("cierra compra");
            estado=2;  
            }     
        // espera compra
        else if(estado==2 && entrada_alcista==1)
            {
            // abre compra y pasa a estado 1
            IOrder order = engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUY, Lots, 0, Slippage);
            print("compra");
            estado=1;
            }               
        } 
    // venta
    else if(estado==3 || estado == 4)
        {
        if(tendencia==1)
            // pasa a estados de compras
            {
            print("pasa a estados compra");    
            if (estado==3)
                 {
                 if(orden(instrument,2) != null) orden(instrument,2).close();   
                 print("cierra venta"); 
                 }
            if(entrada_alcista==1)
                {
                // abre compra y pasa a estado 1
                IOrder order = engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUY, Lots, 0, Slippage);
                print("compra");
                estado=1;
                }               
            else
                {
                //pasa a estado 2
                estado=2;        
                }    
            }
        else if(estado==3 && entrada_bajista==0)
            {
            //cierra venta  y pasa a estado 2
            if(orden(instrument,2) != null) orden(instrument,2).close();
            print("cierre venta");    
            estado=4;  
            }     
        // espera venta
        else if(estado==4 && entrada_bajista==1)
            {
            // abre venta y pasa a estado 3
            IOrder order = engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELL, Lots, 0, Slippage);
            print("venda");
            estado=3;
            }               
                
        }
    //espera venta
    if (estado==4)
        {
            
        }         
    if (estado_old != estado)
        {
        estado_old=estado;
        print (instrument +"-cambio hora"+calendar.get(Calendar.HOUR_OF_DAY)+" dia"+calendar.get(Calendar.DAY_OF_MONTH)+"min "+calendar.get(Calendar.MINUTE)+
        " Estado ="+estado+" tendencia="+tendencia+"entrada_alcista="+entrada_alcista+"entrada_bajista="+entrada_bajista+" Precio="+bidBar.getClose());
        }
    }
}
    
    protected IOrder orden(Instrument instrument, int tipo) throws JFException {
                  
                  
        String label_estrategia;
        for (IOrder order : engine.getOrders(instrument)) 
            {
            label_estrategia= order.getLabel().substring(0,3);                  
            if ((tipo==1) && order.getOrderCommand()==IEngine.OrderCommand.BUY && (label_estrategia.compareTo(strategylabel)>0))
                return order;
            if ((tipo==2) && order.getOrderCommand()==IEngine.OrderCommand.SELL && (label_estrategia.compareTo(strategylabel)>0))
                return order;  
            } 
        return null;    
     } 
    
    protected String getLabel(Instrument instrument) {
        String label = instrument.name();
        label = strategylabel+label.substring(0, 2) + label.substring(3, 5);
        label = label + (tagCounter++);
        label = label.toLowerCase();
        return label;
    }
      

     // count opened positions
    protected int positionsTotal(Instrument instrument, int tipo) throws JFException {
        int counter = 0;
        for (IOrder order : engine.getOrders(instrument)) 
            {
                
            String label_estrategia= order.getLabel().substring(0,3);
            if (order.getState() == IOrder.State.FILLED && (label_estrategia.compareTo(strategylabel)>0)) 
                {
                if (tipo==0)    
                    counter++;
                else if (tipo==1 && order.getOrderCommand()==IEngine.OrderCommand.BUY)
                    counter++;    
                else if (tipo==2 && order.getOrderCommand()==IEngine.OrderCommand.SELL)
                    counter++;    
                }
        }
        return counter;
    }
    
    public void print(Object o) {

        this.console.getOut().println(o.toString());
    }
    private double round(double value, int decimalPlace)
    {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }
}