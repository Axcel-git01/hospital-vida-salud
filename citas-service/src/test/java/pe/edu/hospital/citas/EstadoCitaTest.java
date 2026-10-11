package pe.edu.hospital.citas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EstadoCitaTest {

    @Test
    void pendientePuedeConfirmarseOCancelarse() {
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CONFIRMADA));
        assertTrue(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.CANCELADA));
    }

    @Test
    void pendienteNoPuedeMarcarseAtendida() {
        assertFalse(EstadoCita.PENDIENTE.puedeTransicionarA(EstadoCita.ATENDIDA));
    }

    @Test
    void citaCanceladaNoPuedeConfirmarse() {
        assertTrue(EstadoCita.CANCELADA.puedeTransicionarA(EstadoCita.CONFIRMADA));
    }

    @Test
    void estadosFinalesNoTienenTransiciones() {
        for (EstadoCita destino : EstadoCita.values()) {
            assertFalse(EstadoCita.ATENDIDA.puedeTransicionarA(destino));
            assertFalse(EstadoCita.AUSENTE.puedeTransicionarA(destino));
        }
    }

    @Test
    void soloPendienteYConfirmadaAdmitenReprogramacion() {
        assertTrue(EstadoCita.PENDIENTE.admiteReprogramacion());
        assertTrue(EstadoCita.CONFIRMADA.admiteReprogramacion());
        assertFalse(EstadoCita.ATENDIDA.admiteReprogramacion());
        assertFalse(EstadoCita.CANCELADA.admiteReprogramacion());
        assertFalse(EstadoCita.AUSENTE.admiteReprogramacion());
    }
}
