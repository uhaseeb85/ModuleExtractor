import { cn } from '@/lib/utils'

interface Props {
  children: React.ReactNode
  className?: string
  right?: React.ReactNode
}

export default function SectionHeader({ children, className, right }: Props) {
  return (
    <div className={cn('flex items-center justify-between', className)}>
      <h3 className="section-header">{children}</h3>
      {right && <div>{right}</div>}
    </div>
  )
}
