import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({});

    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    service.clear();
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('should add an informational toast by default', () => {
    const id = service.show({
      message: 'Dados carregados',
    });

    expect(id).toBe(1);
    expect(service.messages()).toEqual([
      {
        id: 1,
        title: undefined,
        message: 'Dados carregados',
        tone: 'info',
        durationMs: 6000,
      },
    ]);
  });

  it('should generate sequential identifiers', () => {
    const firstId = service.show({
      message: 'Primeiro',
      durationMs: 0,
    });

    const secondId = service.show({
      message: 'Segundo',
      durationMs: 0,
    });

    expect(firstId).toBe(1);
    expect(secondId).toBe(2);
  });

  it('should remove a toast after its duration', () => {
    service.show({
      message: 'Operação concluída',
      tone: 'success',
    });

    expect(service.messages()).toHaveLength(1);

    vi.advanceTimersByTime(5999);

    expect(service.messages()).toHaveLength(1);

    vi.advanceTimersByTime(1);

    expect(service.messages()).toHaveLength(0);
  });

  it('should keep danger toasts visible by default', () => {
    service.show({
      message: 'Não foi possível salvar',
      tone: 'danger',
    });

    vi.advanceTimersByTime(60_000);

    expect(service.messages()).toHaveLength(1);
    expect(service.messages()[0]?.tone).toBe('danger');
    expect(service.messages()[0]?.durationMs).toBe(0);
  });

  it('should remove a toast manually', () => {
    const id = service.show({
      message: 'Mensagem removível',
      durationMs: 0,
    });

    service.remove(id);

    expect(service.messages()).toEqual([]);
  });

  it('should remove every toast when clear is called', () => {
    service.show({
      message: 'Primeiro',
      durationMs: 0,
    });

    service.show({
      message: 'Segundo',
      durationMs: 0,
    });

    service.clear();

    expect(service.messages()).toEqual([]);
  });

  it('should cancel the timer when a toast is removed manually', () => {
    const id = service.show({
      message: 'Mensagem temporária',
      durationMs: 1000,
    });

    service.remove(id);
    vi.advanceTimersByTime(1000);

    expect(service.messages()).toEqual([]);
  });
});
