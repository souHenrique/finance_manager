import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransferCreatePage } from './transfer-create-page';

describe('TransferCreatePage', () => {
  let fixture: ComponentFixture<TransferCreatePage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferCreatePage],
    }).compileComponents();

    fixture = TestBed.createComponent(TransferCreatePage);
    fixture.detectChanges();
  });

  it('should render the transfer creation heading and description', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Nova transferência');
    expect(element.querySelector('p')?.textContent).toContain(
      'Transfira valores entre suas contas.',
    );
  });
});
