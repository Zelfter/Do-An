/**
 * Sidebar — collapsible admin navigation (desktop) / drawer (mobile).
 * Presentational only: receives nav items + handlers; no business logic.
 */
import React from 'react'
import { NavLink } from 'react-router-dom'
import { IconChevronLeft, IconClose } from './icons'

export interface SidebarNavItem {
  to: string
  label: string
  icon: React.ReactNode
}

interface SidebarProps {
  brand: string
  navItems: SidebarNavItem[]
  onToggleCollapse: () => void
  mobileOpen: boolean
  onCloseMobile: () => void
  footerNote?: string
}

export default function Sidebar({
  brand, navItems, onToggleCollapse, mobileOpen, onCloseMobile, footerNote,
}: SidebarProps) {
  return (
    <aside className="app-sidebar" aria-label="Điều hướng quản trị">
      <div className="sidebar-brand">
        <IconChevronLeft size={18} />
        <span className="sidebar-brand-text">{brand}</span>
        <button className="sidebar-collapse-btn" onClick={onToggleCollapse} aria-label="Thu gọn menu" type="button">
          <IconChevronLeft size={16} />
        </button>
        <button
          className="sidebar-collapse-btn"
          onClick={onCloseMobile}
          aria-label="Đóng menu"
          type="button"
          style={{ display: mobileOpen ? 'inline-flex' : 'none' }}
        >
          <IconClose size={16} />
        </button>
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}
            onClick={onCloseMobile}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {footerNote && (
        <div className="sidebar-footer">
          <span className="sidebar-footer-text">{footerNote}</span>
        </div>
      )}
    </aside>
  )
}
