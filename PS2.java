package jforex;
import java.math.*;
import java.util.*;

import com.dukascopy.api.*;

public class PS2 implements IStrategy {
    private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	private IUserInterface userInterface;

    private ArrayList<evento> listainstrumentos = new ArrayList<evento>();
    protected double equity;
    protected reporte report;
//    private int estado=2;
//    private int estado_old;
    private int arranque=0;
    private String strategylabel="PS2";
    public double Lots =0.01;
    public double Slippage=3.0;
    private int tagCounter = 0;
    public double profit_totalpasta=0;
    public double drawndown=0;
    public double ultimodrawndown=0;
    public double ddparcial=0;
    public double maximo=0;
    public double equitydd=0;
    public int mesddparcial=0;
    public int diaddparcial=0;
    public int mes_actual=20;
    public int ano_actual=20;
    
    @Configurable ("Debug Mode") public boolean DebugMode=false;
    @Configurable (" MM: 1:Fijo 2:Percent ") public int MMType=2;
    @Configurable (" Riesgo %") public double Riesgo=0.5;    
    @Configurable ("Margen")
    public static int Margen = 0;
    @Configurable ("drawndown en report")
    public static double dd_report = 600;
    @Configurable ("Con logs")
    public static boolean reportar = false;

    public static Instrument []instrumento = new Instrument[10];
    	
	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();
        //evento eurodolar= new evento(Instrument.EURUSD);
        report = new reporte(console,equity,"dolar",dd_report);
        if (DebugMode==false)
            {
            Instrument[] ins= new Instrument[]{
            Instrument.AUDUSD,    
            Instrument.EURCHF,    
            Instrument.EURJPY,    
            Instrument.EURCAD,    
            Instrument.GBPCHF,    
            Instrument.GBPJPY,    
            Instrument.GBPUSD,    
            Instrument.USDCAD,
            Instrument.USDJPY,    
            };
            for (Instrument instrument : ins)
                {
                evento nuevo = new evento(instrument, engine);    
                listainstrumentos.add(nuevo);
                print("instrument nuevo ="+instrument.name());                    
                } 
            arranque=1;           
            }  
        
	}

    public void onAccount(IAccount account) throws JFException {
        this.equity = account.getEquity();
    }

	public void onMessage(IMessage message) throws JFException {
        
    
    //print(calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"MEN:"+message);    
    if(message.getType()==IMessage.Type.ORDER_FILL_OK || message.getType()==IMessage.Type.ORDER_CLOSE_OK || message.getType()==IMessage.Type.ORDER_SUBMIT_OK)
        {
        long tempo=history.getLastTick(message.getOrder().getInstrument()).getTime();        
        if(DebugMode==true) calendar.setTimeInMillis(tempo);  // esto solo es necesario  cuando estamos en backtesting..
        else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
       // print("mensaje para"+message.getOrder().getLabel()+" "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"MEN:"+message);    
        String label_estrategia= message.getOrder().getLabel().substring(0,3);
        if (label_estrategia.compareTo(strategylabel)==0) 
            {
            //print(" 1 mensaje");
            evento ev_mensaje=buscainstrumento(message.getOrder().getInstrument());
            if(message.getType()==IMessage.Type.ORDER_FILL_OK)
                {
                //print(" dos mensaje de order fill");    
                if (message.getOrder().getOrderCommand()==IEngine.OrderCommand.SELL) 
                    {
                    ev_mensaje.estado=2;
                    }
                if (message.getOrder().getOrderCommand()==IEngine.OrderCommand.BUY) 
                    {
                    ev_mensaje.estado=1;
                    }
           // print("mensaje para"+message.getOrder().getLabel()+" "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"MEN:"+message);    
                }
            else if(message.getType()==IMessage.Type.ORDER_CLOSE_OK)
                {
                double diferencia= (ev_mensaje.piv_R2-ev_mensaje.piv_P)* Math.pow(10,ev_mensaje.escala-1);
                double profitpips=0;
                double profitusd=0;    
                Instrument instrument =ev_mensaje.instrumento;    
                if (message.getOrder().getOrderCommand()==IEngine.OrderCommand.SELL) 
                    {
                    profitpips= ev_mensaje.orden_v.getProfitLossInPips();
                    profitusd= ev_mensaje.orden_v.getProfitLossInUSD();                       
                    ev_mensaje.orden_v= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELLLIMIT, lots(diferencia), ev_mensaje.piv_R2, Slippage, 0,ev_mensaje.piv_P);                        
                    ev_mensaje.estado=0;
                //print ("eq="+equity+" a "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"MEN:"+message); 
               //print ("eq="+equity+" y profit="+profit_totalpasta+" a "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)); 
                    }
                if (message.getOrder().getOrderCommand()==IEngine.OrderCommand.BUY) 
                    {
                    profitpips= ev_mensaje.orden_c.getProfitLossInPips();
                    profitusd= ev_mensaje.orden_c.getProfitLossInUSD();                       
                    ev_mensaje.orden_c= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUYLIMIT, lots(diferencia), ev_mensaje.piv_S2, Slippage, 0,ev_mensaje.piv_P);                        
                    ev_mensaje.estado=0;
                //print ("eq="+equity+" a "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"MEN:"+message); 
                //print ("eq="+equity+" y profit="+profit_totalpasta+" a "+calendar.get(Calendar.DAY_OF_MONTH)+"Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)); 
                    }
                ev_mensaje.trades=ev_mensaje.trades+1;
                ev_mensaje.pips=ev_mensaje.pips+profitpips;
                ev_mensaje.pasta=ev_mensaje.pasta+profitusd; 
                // calculo drawn down 
                report.actualiza_reporte(equity, instrument,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
                }     
            }
        }
    if (message.getType()==IMessage.Type.ORDER_CLOSE_OK)
        {
            
            
        }       
	}

	public void onStop() throws JFException {
        
        if(reportar==false) return;    
        report.reporte_final();   
          
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
        
    if (arranque==0)
        {
        if ( DebugMode==true && sistema_activo(tick.getTime()))
            {    
            if (buscainstrumento(instrument)== null)
                {
                evento nuevo = new evento(instrument, engine);    
                listainstrumentos.add(nuevo);
                print("instrument nuevo ="+instrument.name()+" escala="+instrument.getPipScale());
                IBar barra =history.getBar(instrument, Period.WEEKLY, OfferSide.BID,1); 
                nuevo.calcula_pivots(barra);
                print("pivots= "+nuevo.piv_P+" S2="+nuevo.piv_S2+" R2="+nuevo.piv_R2);
                nuevo.estado=maquina_estados(0, nuevo, tick.getBid());    
                    //print("paso por arrance de cero");                      
                }   
            }
        else if ( DebugMode==false && sistema_activo(tick.getTime()))
            {
            evento nuevo2 = buscainstrumento(instrument);   
            if (nuevo2!= null && nuevo2.arranque==0)
                {
                IBar barra =history.getBar(instrument, Period.WEEKLY, OfferSide.BID,1); 
                nuevo2.calcula_pivots(barra);
                print("pivots= "+nuevo2.piv_P+" S2="+nuevo2.piv_S2+" R2="+nuevo2.piv_R2);
                if (nuevo2.orden_c!=null)
                    {    
                    if (nuevo2.orden_c.getOrderCommand() == IEngine.OrderCommand.BUY) 
                        {
                        nuevo2.estado=maquina_estados(1, nuevo2, tick.getBid());      
                        }
                    }    
                if (nuevo2.orden_v!=null)
                    {        
                    if (nuevo2.orden_v.getOrderCommand() == IEngine.OrderCommand.SELL) 
                        {
                        nuevo2.estado=maquina_estados(2, nuevo2, tick.getBid());      
                        }
                    }
                if (nuevo2.orden_c==null && nuevo2.orden_v==null)                
                    {
                    nuevo2.estado=maquina_estados(0, nuevo2, tick.getBid());    
                    //print("paso por arrance de cero");                      
                    }        
                nuevo2.arranque=1;    
                }
            int local=1;    
            for ( evento instru:listainstrumentos)
               if (instru.arranque == 0) local=0;
            if (local==1) arranque=1;
    
            }
        }
	}
	
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        
if (period == period.WEEKLY)
    {      
   evento ev= buscainstrumento(instrument);
   if (ev==null) return;       
   if(DebugMode==true) calendar.setTimeInMillis(bidBar.getTime());  // esto solo es necesario  cuando estamos en backtesting..
   else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));

    ev.calcula_pivots(bidBar);        
//    print(instrument+":"+calendar.get(Calendar.DAY_OF_MONTH)+" Hora"+calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+"estado="+ev.estado
//                +"pivots: P="+ev.piv_P+" , R2="+ev.piv_R2+" , S2="+ev.piv_S2);
    ev.estado=maquina_estados(ev.estado, ev, bidBar.getClose());
    arranque=1;    
    }
if (period == period.ONE_YEAR)
    {
    if(DebugMode==true) calendar.setTimeInMillis(bidBar.getTime());  // esto solo es necesario  cuando estamos en backtesting..
    else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
    if(calendar.get(Calendar.YEAR)!=ano_actual)
        {
        print ("AÑO "+calendar.get(Calendar.YEAR));    
        //report.reporte_periodo(equity,mes[calendar.get(Calendar.MONTH)],"mensual");    
        ano_actual=calendar.get(Calendar.YEAR);
        }    
    }    
        
if (period == period.MONTHLY)
    {
    if(DebugMode==true) calendar.setTimeInMillis(bidBar.getTime());  // esto solo es necesario  cuando estamos en backtesting..
    else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
    if(calendar.get(Calendar.MONTH)!=mes_actual)
        {
        String[] mes= new String[]{
        "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
            
        report.reporte_periodo(equity,mes[calendar.get(Calendar.MONTH)],"mensual");    
        mes_actual=calendar.get(Calendar.MONTH);
        }    
    }    
}

    public int maquina_estados(int estado, evento ev, double precio) throws JFException
    {
        
    Instrument instrument = ev.instrumento;
    double diferencia= (ev.piv_R2-ev.piv_P)* Math.pow(10,ev.escala-1);
    // maquina de estados  
    // estado 0 reposo     
    // estado 1 compra
    // estado 2 venta
    // reposo 0
    if (estado==0)
        {
        // cancela ordenes anteriores    
        //if (ev.orden_c!=null)
        if (ev.orden_c!=null && (ev.orden_c.getState()==IOrder.State.FILLED || ev.orden_c.getState() ==IOrder.State.OPENED))
            {
            //print("estado orden compra"+ev.orden_c.getState()+" label "+ev.orden_c.getLabel());
            ev.orden_c.close();
            }
        if (ev.orden_v!=null && (ev.orden_v.getState()==IOrder.State.FILLED || ev.orden_v.getState() ==IOrder.State.OPENED))
        //if (ev.orden_v!=null)
            {
            //print("estado orden venta"+ev.orden_v.getState()+" label "+ev.orden_v.getLabel());
            ev.orden_v.close();        
            }
        // abre un buylimit S2
        ev.orden_c= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUYLIMIT, lots(diferencia), ev.piv_S2, Slippage, 0,ev.piv_P);
        // abre un sell limit R2
        ev.orden_v= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELLLIMIT, lots(diferencia), ev.piv_R2, Slippage, 0,ev.piv_P);
            
        }
    // compra 1
    else if(estado==1) 
        {
        // cancela orden de venta
        if (ev.orden_v!=null)
            if(ev.orden_v.getState()==IOrder.State.FILLED || ev.orden_v.getState() ==IOrder.State.OPENED)            
            ev.orden_v.close();        
        // abre un selllimit en R2   
        ev.orden_v= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELLLIMIT, lots(diferencia), ev.piv_R2, Slippage, 0,ev.piv_P);
        // modifica el take profit de la compra a PP 
        if(precio< ev.piv_P)
            {
            ev.orden_c.setTakeProfitPrice(ev.piv_P);
            }
        else 
            {
            ev.orden_c.close();
//            ev.orden_c= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUYLIMIT, lots(), ev.piv_S2, Slippage, 0,ev.piv_P);
            return 0;
            }    
        } 
    // venta
    else if(estado==2)
        {
        // cancela orden de compra
        if (ev.orden_c!=null)
         {   
            if(ev.orden_c.getState()==IOrder.State.FILLED || ev.orden_c.getState() ==IOrder.State.OPENED) 
            ev.orden_c.close();
            //print("cancela orden de compra, para hacer una nueva ");
         }   
        // abre un buylimit en S2
        ev.orden_c= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.BUYLIMIT, lots(diferencia), ev.piv_S2, Slippage, 0,ev.piv_P);
        // modifica el take profit de la venta a PP   
        if (precio>ev.piv_P)
            {     
            ev.orden_v.setTakeProfitPrice(ev.piv_P);
            }
        else 
            {
            ev.orden_v.close();
//            ev.orden_v= engine.submitOrder(getLabel(instrument),instrument,IEngine.OrderCommand.SELLLIMIT, lots(), ev.piv_R2, Slippage, 0,ev.piv_P);
            return 0;
            }    
        }
     return estado;   
     }    

    protected double lots(double distancia) throws JFException
    {
    double Lots=0.01;
    if(MMType==1)Lots=0.01;
    else if(MMType==2)
        {
        int enterolotes=(int)(equity/1000);    
        Lots=enterolotes*0.001*Riesgo;   
        if (Lots==0) Lots=0.001; 
        }
    else if (MMType==3) // proporcional a la distancia
        {       
       int entero = (int)(2000/distancia);
        //int entero = (int)(2000/200);
        Lots= entero*0.001;
        if(Lots==0) Lots=0.001;    
        }    
    return Lots;
    }    
    
    protected boolean sistema_activo(long tiempo)
    {
        
    // desactivo de: viernes 22.00 hasta domingo 21.00
    if(DebugMode==true) calendar.setTimeInMillis(tiempo);  // esto solo es necesario  cuando estamos en backtesting..
    else calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
   int hora =calendar.get(Calendar.HOUR_OF_DAY); 
   //int dia =calendar.get(Calendar.DAY_OF_MONTH);
   //int mes =calendar.get(Calendar.MONTH);
   int dia_semana= calendar.get(Calendar.DAY_OF_WEEK);
    if (dia_semana > 1 && dia_semana < 6) return true;
    if ( dia_semana ==1 && hora< 22) return false;
    else if (dia_semana ==6 && hora>= 22) return false;
    else if (dia_semana ==7) return false;
    else return true;
    
    //return true;    
    }
    
    protected String getLabel(Instrument instrument) throws JFException{
        String label = instrument.name();
        label = label.substring(0, 2) + label.substring(3, 5);
        label = label + (tagCounter++);
        label = strategylabel+label.toLowerCase();
        for(IOrder order : engine.getOrders( instrument))
            {
            //String label_estrategia= order.getLabel().substring(0,3);
            if (label.compareTo(order.getLabel())==0)
                {
                label= label + "a";    
                }
            }
        return label;
    }
      

     // count opened positions
    protected int positionsTotal(Instrument instrument, int tipo) throws JFException {
        int counter = 0;
        for (IOrder order : engine.getOrders(instrument)) 
            {
                
            String label_estrategia= order.getLabel().substring(0,3);
            if (order.getState() == IOrder.State.FILLED && (label_estrategia.compareTo(strategylabel)==0)) 
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


    evento buscainstrumento(Instrument instrument)
    {
         for ( evento instru:listainstrumentos)
         {
               if (instru.instrumento == instrument) return instru;
         }    
         return null;
    }
//}

class evento
    {
    private IEngine engine;
    public Instrument instrumento;  
    public int escala;  
    public IOrder orden_c;
    public IOrder orden_v;
    public int estado;
    public int estado_old;
    public double piv_P;
    public double piv_R2;
    public double piv_S2;
    public int trades;
    public double pips;
    public double pasta;
//    public double piv_R3=;
//    public double piv_S3;    
    public String estrategia= "PS2";
    public int arranque;  
    
evento(Instrument instru, IEngine engine2) throws JFException
      {
          //this.engine=context.getEngine();
          this.engine= engine2;
          this.instrumento= instru;
          //this.orden_c=null;
          //this.orden_v=null;
          this.estado=0;
          this.estado_old=0;
          this.trades=0;
          this.pips=0;
          this.pasta=0;
          this.arranque=0;
          this.escala=instru.getPipScale()+1;
          this.orden_c=orden(1);
          this.orden_v=orden(2);
       }  
void calcula_pivots(IBar barra)
      {
        double piv_high = barra.getHigh();
        double piv_low = barra.getLow();  
        double piv_close = barra.getClose();
        this.piv_P= round((piv_high+piv_low+piv_close)/3,escala);
        //this.piv_R2= round(piv_P +piv_high-piv_low,escala);
        
        this.piv_R2= round(piv_P +piv_high-piv_low,escala)- (Margen*instrumento.getPipValue()/10);        
        //this.piv_S2= round(piv_P -piv_high+piv_low,escala);
        this.piv_S2= round(piv_P -piv_high+piv_low,escala)+(Margen*instrumento.getPipValue()/10);
        if (piv_P>piv_R2)
            {
            print(" pivots muy cortos, cierre "+ piv_close+" high="+piv_high+" low="+piv_low);   
            this.piv_R2= round(piv_P +piv_high-piv_low,escala);
            this.piv_S2= round(piv_P -piv_high+piv_low,escala);

            }
//        this.piv_R3;
//        this.piv_S3;          
//        print("s2="+piv_S2+" y mas margen S2="+piv_S21);
      }  
      
/*public double round(double value, int decimalPlace)
    {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }*/
//    protected IOrder orden(Instrument instrument, int tipo) throws JFException{

protected IOrder orden(int tipo) throws JFException{
                  
                  
        String label_estrategia;
        for (IOrder order : engine.getOrders(instrumento)) 
            {
            label_estrategia= order.getLabel().substring(0,3);                  
            if ((tipo==1) && order.getOrderCommand()==IEngine.OrderCommand.BUY && (label_estrategia.compareTo(estrategia)==0))
                return order;
            if ((tipo==2) && order.getOrderCommand()==IEngine.OrderCommand.SELL && (label_estrategia.compareTo(estrategia)==0))
                return order;  
            } 
        return null;    
     }   
}

class reporte
    {
    private IConsole console;
    public double equity_inicial;
    double profit;
    String periodo;
    double drawndown;
    double equity_periodo;
    double dd_maximo;
    double ddparcial;
    double maximo;
    double equitydd;
    int mesddparcial;
    int diaddparcial;
    int anoparcial;

reporte(IConsole consola,double equidad,String str_periodo, double maximo_dd)
    {
        
    this.console=consola;
    this.equity_inicial = equidad;
    this.periodo= str_periodo;
    this.dd_maximo=maximo_dd;   
    this.drawndown=0;
    this.equity_periodo=equidad;
    ddparcial=0; 
    }
                    
void actualiza_reporte(double equidad, Instrument instrument, int ano, int mes, int dia)
    {
    profit = equidad-equity_inicial; 
    if (maximo < profit)
        {
        maximo= profit;
        if(ddparcial>dd_maximo)
             {
             print("  inicio DD parcial ="+ddparcial+"equity="+equitydd+" fecha:"+mesddparcial+" /"+diaddparcial);
             print("  fin DD parcial ="+ddparcial+"equity="+equidad+" fecha:"+ano+" /"+mes+" /"+dia);
             }    
        ddparcial=0;
        }
     if ((maximo-profit)> ddparcial) 
        {
        ddparcial = maximo-profit;
        equitydd=equidad;
        mesddparcial=mes+1;
        diaddparcial=dia;
        anoparcial=ano;   
        }            
      if ((maximo-profit)> drawndown) 
        {
        drawndown = maximo-profit;   
        }   
    }
       
void reporte_periodo(double equidad, String texto, String periodo) 
    {
    print(texto+"="+equidad+" beneficio "+periodo+"="+(equidad-equity_periodo));
    equity_periodo=equidad;    
    }
/*public void print(Object o) {

        this.console.getOut().println(o.toString());
    }*/
           
void reporte_final()
    {
   for ( evento instru:listainstrumentos)
       {
       print("instrumento:"+instru.instrumento+" beneficio="+round(instru.pasta,5)+ " en pips="+round(instru.pips,5)+" de "+instru.trades+" operaciones"); 
       }      
    print("Beneficio en usd="+profit+" y el drawdown= "+drawndown);       
    }
    
}

}

  
    
        
    