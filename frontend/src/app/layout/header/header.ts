import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Header {
  readonly menuOpen = input(false);
  readonly menuToggle = output<void>();
  readonly skipToContent = output<void>();

  private readonly auth = inject(AuthService);

  protected logout(): void {
    this.auth.logout();
  }

  protected focusMainContent(event: MouseEvent): void {
    event.preventDefault();
    this.skipToContent.emit();
  }
}
