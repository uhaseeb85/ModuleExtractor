import { useState, useCallback } from 'react'
import type { ProjectTreeNodeResponse } from '../api/client'
import {
  ChevronRight, ChevronDown, Folder, FolderOpen,
  FileText, FileCode, FileCog, FileBox
} from 'lucide-react'
import { cn } from '@/lib/utils'

function fileIcon(name: string) {
  if (name === 'pom.xml') return FileCog
  if (name.endsWith('.java'))  return FileCode
  if (name.endsWith('.xml'))   return FileBox
  return FileText
}

interface FileTreeNodeProps {
  node: ProjectTreeNodeResponse
  depth?: number
  selectedPath: string | null
  onSelect: (node: ProjectTreeNodeResponse) => void
  defaultOpen?: boolean
}

export default function FileTreeNode({
  node, depth = 0, selectedPath, onSelect, defaultOpen = false,
}: FileTreeNodeProps) {
  const isDir = node.type === 'DIRECTORY'
  const [open, setOpen] = useState(defaultOpen || depth < 2)

  const toggle = useCallback(() => {
    if (isDir) setOpen((v) => !v)
    else onSelect(node)
  }, [isDir, node, onSelect])

  const Icon = isDir
    ? (open ? FolderOpen : Folder)
    : fileIcon(node.name)

  const isSelected = !isDir && selectedPath === node.path

  return (
    <div>
      <button
        onClick={toggle}
        className={cn(
          'flex w-full items-center gap-1.5 rounded-md px-1.5 py-1 text-left text-xs',
          'transition-colors hover:bg-accent/60',
          isSelected && 'bg-primary/10 text-primary font-medium',
        )}
        style={{ paddingLeft: `${depth * 16 + 4}px` }}
      >
        {isDir ? (
          open
            ? <ChevronDown className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
            : <ChevronRight className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
        ) : (
          <span className="h-3.5 w-3.5 shrink-0" />
        )}
        <Icon className={cn(
          'h-3.5 w-3.5 shrink-0',
          isDir ? 'text-amber-500 dark:text-amber-400' : 'text-muted-foreground',
        )} />
        <span className={cn(
          'truncate',
          isDir ? 'font-medium text-foreground' : 'text-muted-foreground',
          node.sourceRef && !node.hasContent && 'italic',
        )}>
          {node.name}
        </span>
        {node.hasContent && !isDir && (
          <span className="ml-auto shrink-0 h-1.5 w-1.5 rounded-full bg-emerald-500" title="Generated" />
        )}
        {node.sourceRef && !node.hasContent && (
          <span className="ml-auto shrink-0 h-1.5 w-1.5 rounded-full bg-blue-400" title="Move from source" />
        )}
      </button>

      {isDir && open && node.children?.map((child) => (
        <FileTreeNode
          key={child.path}
          node={child}
          depth={depth + 1}
          selectedPath={selectedPath}
          onSelect={onSelect}
          defaultOpen={defaultOpen}
        />
      ))}
    </div>
  )
}
