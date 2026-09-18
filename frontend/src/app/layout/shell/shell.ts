import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  inject,
  signal,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from '../header/header';
import { Sidebar } from '../sidebar/sidebar';

@Component({
  selector: 'app-shell',
  imports: [Header, Sidebar, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  protected readonly sidebarOpen = signal(false);

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  protected focusMainContent(): void {
    const mainContent = this.elementRef.nativeElement.querySelector(
      '#main-content',
    ) as HTMLElement | null;

    mainContent?.focus();
  }

  @HostListener('document:keydown.escape')
  protected closeSidebarWithEscape(): void {
    this.closeSidebar();
  }
}
