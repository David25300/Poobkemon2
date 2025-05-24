package PokeBody.domain;

public class Type {
    /**
     * Enum de tipos de Pokémon con matriz de efectividad.
     */
    public enum Tipo {
        NORMAL, FUEGO, AGUA, PLANTA, ELECTRICO, HIELO, LUCHA, VENENO, TIERRA, VOLADOR, PSIQUICO, BICHO, ROCA, FANTASMA, DRAGON, SINIESTRO, ACERO, HADA;
        // Matriz de efectividad [atacante.ordinal()][defensor.ordinal()]
        private static final double[][] EFECTIVIDAD = {
                //            NOR   FUE   AGU  PLA  ELE  HIE  LUC  VEN  TIE  VOL  PSI  BIC  ROC  FAN  DRA  SIN  ACE  HAD
                /*NORMAL*/   {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.5, 0, 1, 1, 0.5, 1},
                /*FUEGO*/    {1, 0.5, 0.5, 2, 1, 2, 1, 1, 1, 1, 1, 2, 0.5, 1, 0.5, 1, 2, 1},
                /*AGUA*/     {1, 2, 0.5, 0.5, 1, 1, 1, 1, 2, 1, 1, 1, 2, 1, 0.5, 1, 1, 1},
                /*PLANTA*/   {1, 0.5, 2, 0.5, 1, 1, 1, 0.5, 2, 0.5, 1, 0.5, 2, 1, 0.5, 1, 0.5, 1},
                /*ELECTRICO*/{1, 1, 2, 0.5, 0.5, 1, 1, 1, 0, 2, 1, 1, 1, 1, 0.5, 1, 1, 1},
                /*HIELO*/    {1, 0.5, 0.5, 2, 1, 0.5, 1, 1, 2, 2, 1, 1, 1, 1, 2, 1, 0.5, 1},
                /*LUCHA*/    {2, 1, 1, 1, 1, 2, 1, 0.5, 1, 0.5, 0.5, 0.5, 2, 0, 1, 2, 2, 0.5},
                /*VENENO*/   {1, 1, 1, 2, 1, 1, 1, 0.5, 0.5, 1, 1, 1, 0.5, 0.5, 1, 1, 0, 2},
                /*TIERRA*/   {1, 2, 1, 0.5, 2, 1, 1, 2, 1, 0, 1, 0.5, 2, 1, 1, 1, 2, 1},
                /*VOLADOR*/  {1, 1, 1, 2, 0.5, 1, 2, 1, 1, 1, 1, 2, 0.5, 1, 1, 1, 0.5, 1},
                /*PSIQUICO*/ {1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 0.5, 1, 1, 1, 1, 0, 0.5, 1},
                /*BICHO*/    {1, 0.5, 1, 2, 1, 1, 0.5, 0.5, 1, 0.5, 2, 1, 1, 0.5, 1, 2, 0.5, 0.5},
                /*ROCA*/     {1, 2, 1, 1, 1, 2, 0.5, 1, 0.5, 2, 1, 2, 1, 1, 1, 1, 0.5, 1},
                /*FANTASMA*/ {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 0.5, 1, 1},
                /*DRAGON*/   {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 0.5, 0},
                /*SINIESTRO*/{1, 1, 1, 1, 1, 1, 0.5, 1, 1, 1, 2, 1, 1, 2, 1, 0.5, 1, 0.5},
                /*ACERO*/    {1, 0.5, 0.5, 1, 0.5, 2, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 0.5, 2},
                /*HADA*/     {1, 0.5, 1, 1, 1, 1, 2, 0.5, 1, 1, 1, 1, 1, 1, 2, 2, 0.5, 1}
        };
        public static double getEfectividad(Tipo atacante, Tipo defensor) {
            return EFECTIVIDAD[atacante.ordinal()][defensor.ordinal()];
        }
    }
    public static double obtenerMultiplicador(Tipo tipoMovimiento, Tipo tipoDefensor1, Tipo tipoDefensor2) {
        double mult1 = Tipo.getEfectividad(tipoMovimiento, tipoDefensor1);
        if (tipoDefensor2 == null) {
            return mult1;
        }
        double mult2 = Tipo.getEfectividad(tipoMovimiento, tipoDefensor2);
        return mult1 * mult2;
    }
}